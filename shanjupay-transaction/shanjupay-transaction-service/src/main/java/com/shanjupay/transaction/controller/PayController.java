package com.shanjupay.transaction.controller;

import com.alibaba.fastjson.JSON;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.*;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.common.util.BrowserType;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.vo.OrderConfirmVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
public class PayController {
    @Reference
    private AppService appService;

    @Reference
    private TransactionService transactionService;

    @RequestMapping("/pay-entry/{ticket}")
    public String payEntry(@PathVariable("ticket") String ticket, HttpServletRequest request) {
        try {
            //将ticket的base64还原
            String ticketJson = EncryptUtil.decodeUTF8StringBase64(ticket);
            //将ticket（json）转成对象
            PayOrderDTO order = JSON.parseObject(ticketJson, PayOrderDTO.class);
            //将对象转成url当中的key-value串格式，供pay.html当中的${RequestParameters['****']调用
            String parameters = ParseURLPairUtil.parseURLPair(order);
            //解析客户端类型
            String header = request.getHeader("user-agent");
            BrowserType browserType = BrowserType.valueOfUserAgent(header);
            switch (browserType) {
                //支付宝
                case ALIPAY:
                    return "forward:/pay-page?" + parameters;
                //微信（待开发）
                case WECHAT:
                    return "forward:/";
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
            //打印错误日志
            log.error(e.getMessage(), e);
        }

        //选择收银台
        return "forward:/pay-page-error";
    }

    @ApiOperation("支付宝门店下单付款")
    @PostMapping("/createAliPayOrder")
    public void createAlipayOrderForStore(OrderConfirmVO orderConfirmVO,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        if (StringUtil.isBlank(orderConfirmVO.getAppId())) {
            throw new BusinessException(CommonErrorCode.E_300003);
        }
        PayOrderDTO payOrderDTO = PayOrderConvert.INSTANCE.vo2dto(orderConfirmVO);
        payOrderDTO.setTotalAmount(Integer.valueOf(AmountUtil.changeY2F(orderConfirmVO.getTotalAmount())));
        payOrderDTO.setClientIp(IPUtil.getIpAddr(request));
        //获取下单应用信息
        AppDTO app = appService.getAppById(payOrderDTO.getAppId());
        //设置所属商户
        payOrderDTO.setMerchantId(app.getMerchantId());
        PaymentResponseDTO payOrderResult = transactionService.submitOrderByAli(payOrderDTO);
        String content = String.valueOf(payOrderResult.getContent());
        log.info("支付宝H5支付响应的结果：" + content);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(content);//直接将完整的表单html输出到页面
        response.getWriter().flush();
        response.getWriter().close();
    }
}
