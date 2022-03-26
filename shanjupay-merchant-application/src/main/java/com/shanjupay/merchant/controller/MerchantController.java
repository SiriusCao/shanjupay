package com.shanjupay.merchant.controller;

import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import com.shanjupay.merchant.convert.MerchantDetailConvert;
import com.shanjupay.merchant.convert.MerchantRegisterConvert;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.service.FileService;
import com.shanjupay.merchant.service.SmsService;
import com.shanjupay.merchant.vo.MerchantDetailVO;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@Api(value = "商户平台‐商户相关", tags = "商户平台‐商户相关", description = "商户平台‐商户相关")
@RestController
@Slf4j
public class MerchantController {
    @Reference
    private MerchantService merchantService;

    @Reference
    private AppService appService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private FileService fileService;

    @ApiOperation("根据id查询商户信息")
    @GetMapping("/merchants/{id}")
    public MerchantDTO queryMerchantById(@PathVariable("id") Long id) {
        MerchantDTO merchantDTO = merchantService.queryMerchantById(id);
        return merchantDTO;
    }

    @ApiOperation("获取登录用户的商户信息")
    @GetMapping("/my/merchants")
    public MerchantDTO getMyMerchantInfo() {
        Long merchantId = SecurityUtil.getMerchantId();
        MerchantDTO merchantDTO = merchantService.queryMerchantById(merchantId);
        return merchantDTO;
    }

    @ApiOperation("获取手机验证码")
    @ApiImplicitParam(name = "phone",
            value = "手机号",
            required = true,
            dataType = "String",
            paramType = "query")
    @GetMapping("/sms")
    public String getSMSCode(@RequestParam("phone") String phone) {
        log.info("向手机号{}发送验证码", phone);
        String key = smsService.sendSMS(phone);
        return key;
    }

    @ApiOperation("注册商户")
    @ApiImplicitParam(name = "merchantRegisterVO",
            value = "注册信息",
            required = true,
            dataType = "MerchantRegisterVO",
            paramType = "body")
    @PostMapping("/merchants/register")
    public MerchantRegisterVO registerMerchant(@RequestBody MerchantRegisterVO merchantRegisterVO) {
        //校检验证码
        smsService.checkVerifiyCode(merchantRegisterVO.getVerifiykey(), merchantRegisterVO.getVerifiyCode());
        //注册商户
        MerchantDTO merchantDTO = MerchantRegisterConvert.INSTANCE.vo2dto(merchantRegisterVO);
        merchantService.createMerchant(merchantDTO);
        return merchantRegisterVO;
    }

    @ApiOperation("证件上传")
    @PostMapping("/upload")
    public String upload(@ApiParam(value = "上传的文件", required = true)
                         @RequestParam("file")
                                 MultipartFile multipartFile) throws IOException {
        //原始文件名称
        String originalFilename = multipartFile.getOriginalFilename();
        //获取文件后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //生成新的唯一的文件名
        String fileName = UUID.randomUUID().toString() + suffix;
        //上传文件
        String fileUrl = fileService.upload(multipartFile.getBytes(), fileName);
        return fileUrl;
    }

    @ApiOperation("商户资质申请")
    @ApiImplicitParam(value = "商户认证资料",
            name = "merchantDetailVO",
            required = true,
            type = "MerchantDetailVO",
            paramType = "body")
    @PostMapping("/my/merchants/save")
    public void saveMerchant(@RequestBody MerchantDetailVO merchantDetailVO) {
        //解析token得到商户id
        Long merchantId = SecurityUtil.getMerchantId();
        //转成DTO
        MerchantDTO merchantDTO = MerchantDetailConvert.INSTANCE.vo2dto(merchantDetailVO);
        //资质申请
        merchantService.applyMerchant(merchantId, merchantDTO);
    }

    @ApiOperation("查询商户下的应用列表")
    @GetMapping("/my/apps")
    public List<AppDTO> queryMyApps() {
        //从Token当中获取merchantId
        Long merchantId = SecurityUtil.getMerchantId();
        //根据merchantId查询所有App
        List<AppDTO> appDTOS = appService.queryAppByMerchant(merchantId);
        return appDTOS;
    }


}
