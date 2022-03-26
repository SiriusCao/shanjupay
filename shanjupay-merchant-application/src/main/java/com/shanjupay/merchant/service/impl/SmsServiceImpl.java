package com.shanjupay.merchant.service.impl;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.PhoneUtil;
import com.shanjupay.common.util.StringUtil;
import com.shanjupay.merchant.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    //nacos配置中心当中的参数
    @Value("${sms.url}")
    private String smsUrl;
    @Value("${sms.effectiveTime}")
    private String smsEffectiveTime;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String sendSMS(String phone) throws BusinessException {
        if (StringUtil.isBlank(phone) || !PhoneUtil.isMatches(phone)){
            throw new BusinessException(CommonErrorCode.E_100109);
        }
        String url = smsUrl + "/generate?name=sms&effectiveTime=" + smsEffectiveTime;
        log.info("调用短信微服务发送验证码:url:{}", url);

        //设置手机号{"mobile":phoneNumber}
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("mobile", phone);

        //设置请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        //json格式
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        //请求信息
        HttpEntity entity = new HttpEntity(body, httpHeaders);


        Map responseMap;
        try {
            //响应信息
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("调用短信微服务发送验证码,返回值:{}", responseEntity);
            responseMap = responseEntity.getBody();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            throw new BusinessException(CommonErrorCode.E_100107);
//            throw new RuntimeException("发送验证码出错");
        }

        if (responseMap == null || responseMap.get("result") == null) {
            throw new BusinessException(CommonErrorCode.E_100107);
//            throw new RuntimeException("发送验证码出错");
        }
        /**
         * responseMap样式为
         * {
         *   "code": 0,
         *   "msg": "正常",
         *   "result": {
         *     "key": "sms:015409cb614c44f4afe9d65484020109",
         *     "content": null
         *   }
         * }
         */
        Map result = (Map) responseMap.get("result");
        String key = result.get("key").toString();
        return key;
    }

    @Override
    public void checkVerifiyCode(String verifiyKey, String verifiyCode) throws BusinessException {
        if (StringUtil.isBlank(verifiyCode) || StringUtil.isBlank(verifiyKey)) {
            throw new BusinessException(CommonErrorCode.E_100103);
        }
        String url = smsUrl + "/verify?name=sms&verificationCode=" + verifiyCode + "&verificationKey=" + verifiyKey;
        log.info("调用短信微服务验证验证码:url:{}", url);
        Map responseMap;
        try {
            //响应信息
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, HttpEntity.EMPTY, Map.class);
            log.info("调用短信微服务验证验证码,返回值:{}", responseEntity);
            //获取响应体
            responseMap = responseEntity.getBody();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            throw new BusinessException(CommonErrorCode.E_100102);
//            throw new RuntimeException("验证码错误");
        }
        if (responseMap == null || responseMap.get("result") == null || !(Boolean) responseMap.get("result")) {
            throw new BusinessException(CommonErrorCode.E_100102);
//            throw new RuntimeException("验证码错误");
        }
    }
}
