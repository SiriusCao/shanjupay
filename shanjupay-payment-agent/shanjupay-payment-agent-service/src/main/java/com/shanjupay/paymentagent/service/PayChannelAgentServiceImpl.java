package com.shanjupay.paymentagent.service;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.paymentagent.api.dto.TradeStatus;
import com.shanjupay.paymentagent.common.constant.AliCodeConstants;
import com.shanjupay.paymentagent.message.PayProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class PayChannelAgentServiceImpl implements PayChannelAgentService {
    @Autowired
    private PayProducer payProducer;

    @Override
    public PaymentResponseDTO createPayOrderByAliWAP(AliConfigParam aliConfigParam, AlipayBean alipayBean) throws BusinessException {
        log.info("支付宝渠道参数" + alipayBean.toString());
        //支付宝渠道参数
        String appId = aliConfigParam.getAppId();
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();
        String charest = aliConfigParam.getCharest();
        String format = aliConfigParam.getFormat();
        String notifyUrl = aliConfigParam.getNotifyUrl();//支付结果通知地址
        String returnUrl = aliConfigParam.getReturnUrl();//支付完成返回商户地址
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();
        String signtype = aliConfigParam.getSigntype();
        String gateway = aliConfigParam.getUrl();//支付宝下单接口地址

        //支付宝sdk客户端
        AlipayClient client = new DefaultAlipayClient(gateway, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype);
        // 封装请求支付信息
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(alipayBean.getOutTradeNo());//闪聚平台订单
        model.setSubject(alipayBean.getSubject());//订单标题
        model.setTotalAmount(alipayBean.getTotalAmount());//订单金额
        model.setBody(alipayBean.getBody());//订单内容
        model.setTimeoutExpress(alipayBean.getExpireTime());//订单过期时间
        model.setProductCode("QUICK_WAP_PAY");//商户与支付宝签定的产品码，固定为 QUICK_WAP_PAY
        alipayRequest.setBizModel(model);//请求参数集合

        String jsonString = JSON.toJSONString(alipayBean);
        log.info("createPayOrderByAliWAP..alipayRequest:{}", jsonString);

        // 设置异步通知地址
        alipayRequest.setNotifyUrl(notifyUrl);
        // 设置同步地址
        alipayRequest.setReturnUrl(returnUrl);
        try {
            // 调用SDK提交表单
            AlipayTradeWapPayResponse response = client.pageExecute(alipayRequest);
            PaymentResponseDTO res = new PaymentResponseDTO();
            res.setContent(response.getBody());

            //发送支付结果查询延迟消息
            PaymentResponseDTO<AliConfigParam> notice=new PaymentResponseDTO<>();
            notice.setOutTradeNo(alipayBean.getOutTradeNo());
            notice.setContent(aliConfigParam);
            notice.setMsg("ALIPAY_WAP");
            payProducer.payOrderNotice(notice);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_400002);
        }
    }


    @Override
    public PaymentResponseDTO queryPayOrderByAli(AliConfigParam aliConfigParam, String outTradeNo) {
        String appId = aliConfigParam.getAppId();
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();
        String charest = aliConfigParam.getCharest();
        String format = aliConfigParam.getFormat();
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();
        String signtype = aliConfigParam.getSigntype();
        String gateway = aliConfigParam.getUrl();//支付宝下单接口地址
        AlipayClient alipayClient = new DefaultAlipayClient(gateway, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype);  //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest(); //创建API对应的request类
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);  //设置业务参数
        PaymentResponseDTO responseDTO;
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request); //通过alipayClient调用API，获得对应的response类
            //接口调用成功
            if (AliCodeConstants.SUCCESSCODE.equals(response.getCode())) {
                //将支付宝响应的状态转换为闪聚平台的状态
                TradeStatus tradeStatus = covertAliTradeStatusToShanjuCode(response.getTradeStatus());
                responseDTO = PaymentResponseDTO.success(response.getTradeNo(),
                        response.getOutTradeNo(),
                        tradeStatus,
                        response.getMsg() + " " + response.getSubMsg());
                log.info("‐‐‐‐查询支付宝H5支付结果" + JSON.toJSONString(responseDTO));
                return responseDTO;
            }
        } catch (AlipayApiException e) {
            log.warn(e.getMessage(), e);
        }
        return PaymentResponseDTO.fail("查询支付宝支付结果异常", outTradeNo, TradeStatus.UNKNOWN);
    }

    /**
     * 将支付宝查询时订单状态trade_status 转换为 闪聚订单状态
     *
     * @param aliTradeStatus 支付宝交易状态
     *                       WAIT_BUYER_PAY（交易创建，等待买家付款）
     *                       TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）
     *                       TRADE_SUCCESS（交易支付成功）
     *                       TRADE_FINISHED（交易结束，不可退款）
     * @return
     */
    private TradeStatus covertAliTradeStatusToShanjuCode(String aliTradeStatus) {
        switch (aliTradeStatus) {
            case AliCodeConstants.WAIT_BUYER_PAY:
                return TradeStatus.USERPAYING;
            case AliCodeConstants.TRADE_SUCCESS:
            case AliCodeConstants.TRADE_FINISHED:
                return TradeStatus.SUCCESS;
            default:
                return TradeStatus.FAILED;
        }
    }
}
