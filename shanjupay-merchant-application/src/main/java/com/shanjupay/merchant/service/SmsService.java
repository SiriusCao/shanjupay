package com.shanjupay.merchant.service;

import com.shanjupay.common.domain.BusinessException;

public interface SmsService {
    /**
     * 获取短信验证码
     *
     * @param phone
     * @return
     */
    String sendSMS(String phone) throws BusinessException;

    /**
     * 校检验证码
     *
     * @param verifiyKey
     * @param verifiyCode
     */
    void checkVerifiyCode(String verifiyKey, String verifiyCode) throws BusinessException;
}
