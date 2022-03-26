package com.shanjupay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.RandomUuidUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.convert.AppConvert;
import com.shanjupay.merchant.entity.App;
import com.shanjupay.merchant.entity.Merchant;
import com.shanjupay.merchant.mapper.AppMapper;
import com.shanjupay.merchant.mapper.MerchantMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
@Slf4j
public class AppServiceImpl implements AppService {
    @Autowired
    private AppMapper appMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    /**
     * 检验应用名是否已被使用
     *
     * @param appName 待查询的APPName
     * @return 此APPName是否已被使用
     */
    public Boolean isExiestAppnName(String appName) {
        //构建条件lambda表达式
        LambdaQueryWrapper<App> lambdaQueryWrapper =
                new LambdaQueryWrapper<App>().eq(App::getAppName, appName);
        Integer count = appMapper.selectCount(lambdaQueryWrapper);
        return count > 0;
    }

    @Override
    public List<AppDTO> queryAppByMerchant(Long merchantID) throws BusinessException {
        //构造条件表达式，根据merchantId查询
        LambdaQueryWrapper<App> lambdaQueryWrapper =
                new LambdaQueryWrapper<App>().eq(App::getMerchantId, merchantID);
        List<App> apps = appMapper.selectList(lambdaQueryWrapper);
        //将App的list转行成APPDTO的list
        List<AppDTO> appDTOS = AppConvert.INSTANCE.listentity2dto(apps);
        return appDTOS;
    }

    @Override
    public AppDTO getAppById(String id) throws BusinessException {
        //构造条件根绝APPId检查
        LambdaQueryWrapper<App> lambdaQueryWrapper =
                new LambdaQueryWrapper<App>().eq(App::getAppId, id);
        App app = appMapper.selectOne(lambdaQueryWrapper);
        //将App转换成APPDTO
        AppDTO appDTO = AppConvert.INSTANCE.entity2dto(app);
        return appDTO;
    }

    @Override
    public AppDTO createApp(Long merchantId, AppDTO appDTO) throws BusinessException {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(CommonErrorCode.E_200002);
        }
        //校检商户是否通过资质审核
        if (!"2".equals(merchant.getAuditStatus())) {
            throw new BusinessException(CommonErrorCode.E_200003);
        }
        //检测AppName是否已经存在
        if (isExiestAppnName(appDTO.getAppName())) {
            throw new BusinessException(CommonErrorCode.E_200004);
        }
        //dto转换entity
        App app = AppConvert.INSTANCE.dto2entity(appDTO);
        //生成APPId
        String appId = RandomUuidUtil.getUUID();
        app.setAppId(appId);
        app.setMerchantId(merchantId);
        //保存应用信息
        appMapper.insert(app);
        return AppConvert.INSTANCE.entity2dto(app);

    }

    @Override
    public Boolean queryAppInMerchant(String appId, Long merchantId) throws BusinessException {
        LambdaQueryWrapper<App> lambdaQueryWrapper=
                new LambdaQueryWrapper<App>()
                .eq(App::getAppId,appId)
                .eq(App::getMerchantId,merchantId);
        Integer count = appMapper.selectCount(lambdaQueryWrapper);
        return count>0;
    }
}
