package com.shanjupay.merchant.service;

import com.shanjupay.common.domain.BusinessException;


public interface FileService {
    /**
     * 文件上传
     * @param bytes 文件字符数组
     * @param fileName 生成的唯一文件名
     * @return 文件的七牛云存储连接
     * @throws BusinessException 自定义异常
     */
    String upload(byte[] bytes, String fileName) throws BusinessException;
}
