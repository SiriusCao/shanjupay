package com.shanjupay.merchant.controller;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.common.util.QRCodeUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@Api(value = "商户平台‐门店管理", tags = "商户平台‐门店管理", description = "商户平台‐门店的增删改 查")
@Slf4j
@RestController
public class StoreController {
    //门店二维码订单标题
    @Value("${shanjupay.c2b.subject}")
    private String subject;
    //门店二维码订单内容
    @Value("${shanjupay.c2b.body}")
    private String body;

    @Reference
    private TransactionService transactionService;

    @Reference
    private MerchantService merchantService;

    @ApiOperation("分页条件查询商户下门店")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true, dataType = "int", paramType = "query")
    })
    @PostMapping("/my/stores/merchants/page")
    public PageVO<StoreDTO> queryStoreByPage(@RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        Long merchantId = SecurityUtil.getMerchantId();
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setMerchantId(merchantId);
        PageVO<StoreDTO> pageVO = merchantService.queryStoreByPage(storeDTO, pageNo, pageSize);
        return pageVO;
    }


    @ApiOperation("生成商户应用门店二维码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "商户应用id", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "storeId", value = "商户门店id", required = true, dataType = "String", paramType = "path")
    })
    @GetMapping(value = "/my/apps/{appId}/stores/{storeId}/app-store-qrcode")
    public String createCScanBStoreQRCode(@PathVariable("appId") String appId,
                                          @PathVariable("storeId") Long storeId) throws BusinessException {
        //商户id
        Long merchantId = SecurityUtil.getMerchantId();
        //生成二维码链接
        QRCodeDto qrCodeDto = new QRCodeDto();
        qrCodeDto.setAppId(appId);
        qrCodeDto.setStoreId(storeId);
        qrCodeDto.setMerchantId(merchantId);

        MerchantDTO merchantDTO = merchantService.queryMerchantById(merchantId);
        //"%s 商品"
        String newSubject = String.format(subject, merchantDTO.getMerchantName());
        qrCodeDto.setSubject(newSubject);
        //内容,格式："向%s 付款"
        String newBody = String.format(body, merchantDTO.getMerchantName());
        qrCodeDto.setBody(newBody);
        //生成门店二维码
        String storeQRCode = transactionService.createStoreQRCode(qrCodeDto);
        log.info("[merchantId:{},appId:{},storeId:{}]createCScanBStoreQRCode is {}", merchantId, appId, storeId, storeQRCode);
        try {
            QRCodeUtil qrCodeUtil = new QRCodeUtil();
            return qrCodeUtil.createQRCode(storeQRCode, 200, 200);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.E_200007);
        }
    }

}
