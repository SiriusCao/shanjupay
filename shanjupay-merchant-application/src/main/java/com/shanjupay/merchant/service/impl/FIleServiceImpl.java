package com.shanjupay.merchant.service.impl;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.QiniuUtils;
import com.shanjupay.merchant.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FIleServiceImpl implements FileService {
    /**
     * 上传七牛云所需的参数
     */
    @Value("${oss.qiniu.url}")
    private String url;
    @Value("${oss.qiniu.accessKey}")
    private String accessKey;
    @Value("${oss.qiniu.secretKey}")
    private String secretKey;
    @Value("${oss.qiniu.bucket}")
    private String bucket;

    @Override
    public String upload(byte[] bytes, String fileName)throws BusinessException{
        try{
            //将文件上传到七牛云存储服务
            QiniuUtils.upload2Qiniu(accessKey,secretKey,bucket,bytes,fileName);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            throw new BusinessException(CommonErrorCode.E_100106);
        }
        //返回url+文件名
        return url+fileName;
    }
}
