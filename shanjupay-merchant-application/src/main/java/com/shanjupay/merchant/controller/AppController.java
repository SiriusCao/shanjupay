package com.shanjupay.merchant.controller;

import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import com.shanjupay.transaction.api.PayChannelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

@Api(value = "商户平台‐应用管理", tags = "商户平台‐应用管理", description = "商户平台‐应用管理")
@RestController
public class AppController {
    @Reference
    private AppService appService;

    @Reference
    private PayChannelService payChannelService;

    @ApiOperation("商户创建应用")
    @ApiImplicitParam(name = "appDTO",
            value = "应用信息",
            required = true,
            dataType = "AppDTO",
            paramType = "body")
    @PostMapping("/my/apps")
    public AppDTO createApp(@RequestBody AppDTO appDTO) {
        //获取merchantId
        Long merchantId = SecurityUtil.getMerchantId();
        return appService.createApp(merchantId, appDTO);
    }


    @ApiOperation("根据appid获取应用的详细信息")
    @ApiImplicitParam(name = "appId",
            value = "商户应用ID",
            required = true,
            dataType = "String",
            paramType = "path")
    @GetMapping("/my/apps/{appId}")
    public AppDTO getApp(@PathVariable(value = "appId") String appId) {
        AppDTO appDTO = appService.getAppById(appId);
        return appDTO;
    }

    @ApiOperation("绑定服务类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用id", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "platformChannelCodes", value = "服务类型code", required = true, dataType = "String", paramType = "query")
    })
    @PostMapping("/my/apps/{appId}/platform‐channels")
    public void bindPlatformForApp(@PathVariable("appId") String appId,
                                   @RequestParam("platformChannelCodes") String platformChannelCodes) {
        payChannelService.bindPlatformChannelForApp(appId, platformChannelCodes);
    }

    @ApiOperation("查询应用是否绑定了某个服务类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用id", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "platformChannelCodes", value = "服务类型", required = true, dataType = "String", paramType = "query")
    })
    @GetMapping("/my/merchants/apps/platformchannels")
    public int queryAppBindPlatformChannel(@RequestParam String appid,
                                           @RequestParam String platformChannel) {
        return payChannelService.queryAppBindPlatformChannel(appid, platformChannel);
    }


}
