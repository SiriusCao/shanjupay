package com.shanjupay.merchant.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.merchant.api.dto.AppDTO;

import java.util.List;

public interface AppService {
    /**
     * 商户创建应用
     *
     * @param merchantId
     * @param app
     * @return
     * @throws BusinessException
     */
    AppDTO createApp(Long merchantId, AppDTO appDTO) throws BusinessException;

    /**
     * 根据商户id查询App
     *
     * @param merchantID
     * @return
     * @throws BusinessException
     */
    List<AppDTO> queryAppByMerchant(Long merchantID) throws BusinessException;

    /**
     * 根据id查询app
     *
     * @param id
     * @return
     * @throws BusinessException
     */
    AppDTO getAppById(String id) throws BusinessException;

    /**
     * 查询应用是否属于某个商户
     *
     * @param appId
     * @param merchantId
     * @return
     * @throws BusinessException
     */
    Boolean queryAppInMerchant(String appId, Long merchantId) throws BusinessException;
}
