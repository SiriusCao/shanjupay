package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.cache.Cache;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.RedisUtil;
import com.shanjupay.common.util.StringUtil;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.dto.PayChannelDTO;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PlatformChannelDTO;
import com.shanjupay.transaction.convert.PayChannelParamConvert;
import com.shanjupay.transaction.convert.PlatformChannelConvert;
import com.shanjupay.transaction.entity.AppPlatformChannel;
import com.shanjupay.transaction.entity.PayChannelParam;
import com.shanjupay.transaction.entity.PlatformChannel;
import com.shanjupay.transaction.mapper.AppPlatformChannelMapper;
import com.shanjupay.transaction.mapper.PayChannelParamMapper;
import com.shanjupay.transaction.mapper.PlatformChannelMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class PayChannelServiceImpl implements PayChannelService {
    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private AppPlatformChannelMapper appPlatformChannelMapper;

    @Autowired
    private PayChannelParamMapper payChannelParamMapper;

    @Resource
    private Cache cache;

    @Override
    public List<PlatformChannelDTO> queryPlatformChannel() throws BusinessException {
        //查询所有的platformChannel
        List<PlatformChannel> platformChannels =
                platformChannelMapper.selectList(null);
        //转成DTO
        List<PlatformChannelDTO> platformChannelDTOS =
                PlatformChannelConvert.INSTANCE.listentity2listdto(platformChannels);
        return platformChannelDTOS;
    }

    @Override
    @Transactional
    public void bindPlatformChannelForApp(String appId, String platformChannelCodes) throws BusinessException {
        //根据appId和平台服务类型code查询app_platform_channel
        LambdaQueryWrapper<AppPlatformChannel> lambdaQueryWrapper =
                new LambdaQueryWrapper<AppPlatformChannel>()
                        .eq(AppPlatformChannel::getAppId, appId)
                        .eq(AppPlatformChannel::getPlatformChannel, platformChannelCodes);
        AppPlatformChannel appPlatformChannel = appPlatformChannelMapper.selectOne(lambdaQueryWrapper);
        //如果没有绑定则绑定
        if (appPlatformChannel == null) {
            appPlatformChannel = new AppPlatformChannel();
            appPlatformChannel.setAppId(appId);
            appPlatformChannel.setPlatformChannel(platformChannelCodes);
            appPlatformChannelMapper.insert(appPlatformChannel);
        }
    }

    @Override
    public int queryAppBindPlatformChannel(String appId, String platformChannel) throws BusinessException {
        LambdaQueryWrapper<AppPlatformChannel> lambdaQueryWrapper =
                new LambdaQueryWrapper<AppPlatformChannel>()
                        .eq(AppPlatformChannel::getAppId, appId)
                        .eq(AppPlatformChannel::getPlatformChannel, platformChannel);
        Integer count = appPlatformChannelMapper.selectCount(lambdaQueryWrapper);
        //已绑定返回1，否则 返回0
        return count > 0 ? 1 : 0;
    }

    @Override
    public List<PayChannelDTO> queryPayChannelByPlatformChannel(String platformChannelCode) throws BusinessException {
        List<PayChannelDTO> payChannelDTOS = platformChannelMapper.selectPayChannelByPlatformChannel(platformChannelCode);
        return payChannelDTOS;
    }

    @Override
    public void savePayChannelParam(PayChannelParamDTO payChannelParamDTO) throws BusinessException {
        //检验数据合法性
        if (payChannelParamDTO == null
                || StringUtil.isBlank(payChannelParamDTO.getAppId())
                || StringUtil.isBlank(payChannelParamDTO.getPlatformChannelCode())
                || StringUtil.isBlank(payChannelParamDTO.getPayChannel())) {
            throw new BusinessException(CommonErrorCode.E_300009);
        }
        //根据 appid 和 服务类型 查询 应用与服务类型绑定id(appPlatformChannelId)
        Long appPlatformChannelId = selectIdByAppPlatformChannel(
                payChannelParamDTO.getAppId(),
                payChannelParamDTO.getPlatformChannelCode());
        //应用未绑定该服务类型不可进行支付渠道参数配置
        if (appPlatformChannelId == null) {
            throw new BusinessException(CommonErrorCode.E_300010);
        }
        //根据 应用与服务类型绑定id(appPlatformChannelId) 和 支付渠道 查询 参数信息
        LambdaQueryWrapper<PayChannelParam> lambdaQueryWrapper =
                new LambdaQueryWrapper<PayChannelParam>()
                        .eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId)
                        .eq(PayChannelParam::getPayChannel, payChannelParamDTO.getPayChannel());
        PayChannelParam payChannelParam = payChannelParamMapper.selectOne(lambdaQueryWrapper);
        //更新已有配置
        if (payChannelParam != null) {
            payChannelParam.setChannelName(payChannelParamDTO.getChannelName());
            payChannelParam.setParam(payChannelParamDTO.getParam());
            payChannelParamMapper.updateById(payChannelParam);
        } else {
            //添加新配置
            payChannelParam =
                    PayChannelParamConvert.INSTANCE.dto2entity(payChannelParamDTO);
            payChannelParam.setId(null);
            payChannelParam.setAppPlatformChannelId(appPlatformChannelId);
            payChannelParamMapper.insert(payChannelParam);
        }
        //更新缓存
        updateCache(payChannelParamDTO.getAppId(), payChannelParamDTO.getPlatformChannelCode());
    }

    /**
     * 更新redis当中的数据
     *
     * @param appId           应用
     * @param platformChannel 服务
     */
    public void updateCache(String appId, String platformChannel) {
        //key的构建
        String redisKey = RedisUtil.keyBuilder(appId, platformChannel);
        //查询redis中是否已经存在，如果存在则删除
        Boolean exists = cache.exists(redisKey);
        if (exists) {
            cache.del(redisKey);
        }
        //从数据库查询应用的服务类型对应的实际支付参数，并重新存入缓存
        List<PayChannelParamDTO> payChannelParamDTOS =
                selectparamDTOByAppIdAndPlatform(appId, platformChannel);
        if (payChannelParamDTOS != null) {
            cache.set(redisKey, JSON.toJSON(payChannelParamDTOS).toString());
        }
    }

    /**
     * 根据 AppID 和 服务类型 查询AppPlatformChannel表的id
     *
     * @param appId               应用id
     * @param platformChannelCode 服务类型
     * @return AppPlatformChannel表的id
     */
    private Long selectIdByAppPlatformChannel(String appId, String platformChannelCode) {
        LambdaQueryWrapper<AppPlatformChannel> lambdaQueryWrapper =
                new LambdaQueryWrapper<AppPlatformChannel>()
                        .eq(AppPlatformChannel::getAppId, appId)
                        .eq(AppPlatformChannel::getPlatformChannel, platformChannelCode);
        AppPlatformChannel appPlatformChannel = appPlatformChannelMapper.selectOne(lambdaQueryWrapper);
        if (appPlatformChannel != null) {
            return appPlatformChannel.getId();
        }
        return null;
    }

    @Override
    public List<PayChannelParamDTO> queryPayChannelParamByAppAndPlatform(
            String appId,
            String platformChannelCode) throws BusinessException {
        //构建key
        String redisKey = RedisUtil.keyBuilder(appId, platformChannelCode);
        //查询是否存在，如果存在 则转型之后返回
        Boolean exists = cache.exists(redisKey);
        if (exists) {
            //获取value
            String value = cache.get(redisKey);
            //将value转型成对象
            List<PayChannelParamDTO> payChannelParamDTOS =
                    JSONObject.parseArray(value, PayChannelParamDTO.class);
            return payChannelParamDTOS;
        }
        //根据应用和服务查询PayChannelParamDTO
        List<PayChannelParamDTO> payChannelParamDTOS =
                selectparamDTOByAppIdAndPlatform(appId, platformChannelCode);
        //添加到redis
        cache.set(redisKey,JSON.toJSON(payChannelParamDTOS).toString());
        return payChannelParamDTOS;
    }

    /**
     * 根据应用和服务查询PayChannelParamDTO
     *
     * @param appId               应用
     * @param platformChannelCode 服务
     * @return
     */
    public List<PayChannelParamDTO> selectparamDTOByAppIdAndPlatform(String appId, String platformChannelCode) {
        //根据 AppID 和 服务类型 查询AppPlatformChannel表的id
        Long appPlatformChannelId = selectIdByAppPlatformChannel(appId, platformChannelCode);
        //如果为空说明该应用未和该服务绑定
        if (appPlatformChannelId == null) {
            throw new BusinessException(CommonErrorCode.E_300010);
        }
        //根据appPlatformChannelId查询所有该应用所绑定的 支付渠道参数（可能会有多个）
        LambdaQueryWrapper<PayChannelParam> lambdaQueryWrapper =
                new LambdaQueryWrapper<PayChannelParam>()
                        .eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId);
        List<PayChannelParam> payChannelParams =
                payChannelParamMapper.selectList(lambdaQueryWrapper);
        //将查询的结构转成DTO
        List<PayChannelParamDTO> payChannelParamDTOS =
                PayChannelParamConvert.INSTANCE.listentity2listdto(payChannelParams);
        return payChannelParamDTOS;
    }

    @Override
    public PayChannelParamDTO queryParamByAppPlatformAndPayChannel(
            String appId,
            String platformChannelCode,
            String payChannel) throws BusinessException {
        //根据 AppID 和 服务类型 查询AppPlatformChannel表的id
        Long appPlatformChannelId = selectIdByAppPlatformChannel(appId, platformChannelCode);
        //如果为空说明该应用未和该服务绑定
        if (appPlatformChannelId == null) {
            throw new BusinessException(CommonErrorCode.E_300010);
        }
        //根据appPlatformChannelId和payChannel和查询该应用所绑定的 支付渠道参数(唯一的)
        LambdaQueryWrapper<PayChannelParam> lambdaQueryWrapper =
                new LambdaQueryWrapper<PayChannelParam>()
                        .eq(PayChannelParam::getAppPlatformChannelId, appPlatformChannelId)
                        .eq(PayChannelParam::getPayChannel, payChannel);
        PayChannelParam payChannelParam = payChannelParamMapper.selectOne(lambdaQueryWrapper);
        return PayChannelParamConvert.INSTANCE.entity2dto(payChannelParam);
    }
}
