package com.shanjupay.transaction.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.transaction.api.dto.PayChannelDTO;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PlatformChannelDTO;

import java.util.List;

public interface PayChannelService {
    /**
     * 获取所有平台服务类型
     *
     * @return
     * @throws BusinessException
     */
    List<PlatformChannelDTO> queryPlatformChannel() throws BusinessException;

    /**
     * 为app绑定平台服务类型
     *
     * @param appId
     * @param platformChannelCodes
     * @throws BusinessException
     */
    void bindPlatformChannelForApp(String appId, String platformChannelCodes) throws BusinessException;

    /**
     * 应用是否已经绑定了某个服务类型
     *
     * @param appId
     * @param platformChannelCodes
     * @return 已绑定返回1，否则 返回0
     * @throws BusinessException
     */
    int queryAppBindPlatformChannel(String appId, String platformChannelCodes) throws BusinessException;

    /**
     * 根据平台服务类型获取支付渠道列表
     *
     * @param platformChannelCode
     * @return
     * @throws BusinessException
     */
    List<PayChannelDTO> queryPayChannelByPlatformChannel(String platformChannelCode) throws BusinessException;

    /**
     * 保存支付渠道参数
     *
     * @param payChannelParamDTO 商户原始支付渠道参数
     */
    void savePayChannelParam(PayChannelParamDTO payChannelParamDTO) throws BusinessException;

    /**
     * 获取指定应用指定服务类型下所包含的原始支付渠道参数列表
     *
     * @param appId               应用id
     * @param platformChannelCode 服务类型
     * @return 多个结果
     */
    List<PayChannelParamDTO> queryPayChannelParamByAppAndPlatform(String appId,
                                                                  String platformChannelCode) throws BusinessException;

    /**
     * 获取指定应用指定服务类型下所包含的某个原始支付参数
     *
     * @param appId               应用id
     * @param platformChannelCode 服务类型
     * @param payChannel          支付渠道
     * @return 和上面的方法不同的是只有一个结果
     */
    PayChannelParamDTO queryParamByAppPlatformAndPayChannel(String appId,
                                                            String platformChannelCode,
                                                            String payChannel) throws BusinessException;
}
