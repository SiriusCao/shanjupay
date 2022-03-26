package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.entity.PayOrder;
import com.shanjupay.transaction.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    //支付入口url
    @Value("${shanjupay.payurl}")
    private String payUrl;

    @Reference
    private AppService appService;

    @Reference
    private MerchantService merchantService;

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private PayChannelService payChannelService;

    @Reference
    private PayChannelAgentService payChannelAgentService;

    /**
     * 生成门店二维码
     *
     * @param qrCodeDto 传入merchantId,appId、storeid、channel、subject、body
     * @return 支付入口URL，将二维码的参数组成json并用base64编码
     * @throws BusinessException 自定义异常
     */
    @Override
    public String createStoreQRCode(QRCodeDto qrCodeDto) throws BusinessException {
        //校验应用和门店
        verifyAppAndStore(qrCodeDto.getMerchantId(), qrCodeDto.getAppId(), qrCodeDto.getStoreId());
        //生成支付信息
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setMerchantId(qrCodeDto.getMerchantId());
        payOrderDTO.setAppId(qrCodeDto.getAppId());
        payOrderDTO.setStoreId(qrCodeDto.getStoreId());
        payOrderDTO.setSubject(qrCodeDto.getSubject());
        payOrderDTO.setChannel("shanju_c2b");
        payOrderDTO.setBody(qrCodeDto.getBody());
        String jsonString = JSON.toJSONString(payOrderDTO);
        log.info("transaction service createStoreQRCode,JsonString is {}", jsonString);
        //将支付信息保存到票据中
        String ticket = EncryptUtil.encodeUTF8StringBase64(jsonString);
        //支付入口
        String payEntryUrl = payUrl + ticket;
        log.info("transaction service createStoreQRCode,pay‐entry is {}", payEntryUrl);
        return payEntryUrl;
    }

    /*
    校验应用和门店是否属于当前登录商户
     */
    public void verifyAppAndStore(Long merchantId, String appId, Long storeID) {
        if (!appService.queryAppInMerchant(appId, merchantId)) {
            throw new BusinessException(CommonErrorCode.E_200005);
        }
        if (!merchantService.queryStoreInMerchant(storeID, merchantId)) {
            throw new BusinessException(CommonErrorCode.E_200006);
        }
    }

    @Override
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) throws BusinessException {
        //保存订单
        payOrderDTO.setPayChannel("ALIPAY_WAP");
        //保存订单
        payOrderDTO = save(payOrderDTO);
        //调用支付代理服务请求第三方支付系统
        return alipayH5(payOrderDTO.getTradeNo());
    }

    /**
     * 保存订单到闪聚平台
     *
     * @param payOrderDTO
     * @return
     */
    private PayOrderDTO save(PayOrderDTO payOrderDTO) {
        PayOrder payOrder = PayOrderConvert.INSTANCE.dto2entity(payOrderDTO);
        payOrder.setTradeNo(PaymentUtil.genUniquePayOrderNo());
        payOrder.setCreateTime(LocalDateTime.now());
        //设置过期时间，30分钟
        payOrder.setExpireTime(LocalDateTime.now().plus(30, ChronoUnit.MINUTES));
        payOrder.setCurrency("CNY");//设置支付币种
        payOrder.setTradeState("0");//订单状态
        int insert = payOrderMapper.insert(payOrder);
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    //调用支付宝下单接口
    private PaymentResponseDTO alipayH5(String tradeNo) {
        //构建支付实体
        AlipayBean alipayBean = new AlipayBean();
        //根据订单号查询订单详情
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);
        alipayBean.setOutTradeNo(tradeNo);
        alipayBean.setSubject(payOrderDTO.getSubject());
        String totalAmount = null;

        //支付宝那边入参是元
        try {
            //将分转成元
            totalAmount = AmountUtil.changeF2Y(payOrderDTO.getTotalAmount().toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_300006);
        }
        alipayBean.setTotalAmount(totalAmount);
        alipayBean.setBody(payOrderDTO.getBody());
        alipayBean.setStoreId(payOrderDTO.getStoreId());
        alipayBean.setExpireTime("30m");

        //根据应用、服务类型、支付渠道查询支付渠道参数
        PayChannelParamDTO payChannelParamDTO =
                payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(),
                        payOrderDTO.getChannel(),
                        "ALIPAY_WAP");
        if (payChannelParamDTO == null) {
            throw new BusinessException(CommonErrorCode.E_300007);
        }
        //支付宝渠道参数
        AliConfigParam aliConfigParam = JSON.parseObject(payChannelParamDTO.getParam(), AliConfigParam.class);
        //字符编码
        aliConfigParam.setCharest("utf-8");
        PaymentResponseDTO payOrderResponse = payChannelAgentService.createPayOrderByAliWAP(aliConfigParam, alipayBean);
        log.info("支付宝H5支付响应Content:" + payOrderResponse.getContent());
        return payOrderResponse;
    }

    @Override
    public PayOrderDTO queryPayOrder(String tradeNo) {
        PayOrder payOrder = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().lambda().eq(PayOrder::getTradeNo, tradeNo)
        );
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }


    @Override
    public void updateOrderTradeNoAndTradeState(String tradeNo, String payChannelTradeNo, String state) {
        LambdaUpdateWrapper<PayOrder> lambdaQueryWrapper =
                new LambdaUpdateWrapper<PayOrder>()
                        .eq(PayOrder::getTradeNo, tradeNo)
                        .set(PayOrder::getPayChannelTradeNo, payChannelTradeNo)
                        .set(PayOrder::getTradeState, state);
        if (state != null && "2".equals(state)) {
            lambdaQueryWrapper.set(PayOrder::getPaySuccessTime, LocalDateTime.now());
        }
        payOrderMapper.update(null,lambdaQueryWrapper);
    }
}
