package com.shanjupay.transaction.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class PayTestController {
    String APP_ID = "2021000119645160";
    String APP_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCA+FBv9hEjtd26ylPoWXHEhbqGm0iEgx15yOmmdJE4rChZBvYu5GxA56aietvGGfWQ6mJR931svlCbNxj9bQtHcd3nxvAo87moBOGT0fW/lL3+C/04ENYOX/etI5qIYb5g2nfRwqWTuYrykk/i0k/cEce2nC2WK95IGMK3G7WGOLZu7V+Daw2tIUqhkVtBMRIdjnQcrpOPhunzZoTSakx13o4Agne/9FwRfF7elv5TVkntUP8uLyvO8jJQGu5lNeTUAh792NbhhCyxKnPJMkfLtoDfpI5b2acXSqRvAj6c6lqPR3pO93QgUegQ2bimCxUMdErplkdLp2x7hJ+Ckc7vAgMBAAECggEAbdgQWmuPyYR6Zz8wG/MibKkhZsgXCZXKoxE1v6oEjepDKyA8yU+Py/ABAt31FVLCzjxypTFPSDEH4ksZI6+eLamTwHa10YtUEwClSAtJbXS4JQn9D6V3SyL9hh+O3J2zUjaAWs2XzKv45gUruRo7HkqeeK+oPAD8/xbnsKEPCFM/zMOkXw5juKNy0fW9X8Lu3MTaPXq1/Fc6UexN1QjGENBSuZxR+idX3JWYH4kq5J5Ku+gZCkfshialwnrrpe/4GQpXZt/yCxTQWK6KM+RA0gC+9fm6p5GnKP4B2tzHAzJN2IgmMsr6agtiWLgwP++mUtHf2JA567TRMlYJX+MHYQKBgQDvn4g9D7aGt85XVJunUzUT92On2YKYw8QOZNXZ0MSmeTMQhxsQUd15lYTlthUHuI9TNf1CpQvqY0/iaoQqrJ6r6j1sgIel6enJYy6QLLAY1tY1H5QctFICJZpy7QnLx/+1VDXV+9Q3jaJN+x+Tvis905DUkqRtvpTkoOcmY9HaFwKBgQCJyMi8QY4B8NL1HEAWSloJivw7mUVyCSqnDBlxWZ6NiNhF8XXOl8Xq4KhWYSSaFw1gSJA1IITeDXZIoHzzuhQUnL5R+Bin6eUqhzLwfOELPKww0z25/kiWikB8GIl1uKGW6uQBKDHqoPyv7K1OobQ0g+ku1jAJzLMmLZg38IIw6QKBgFJFPrxgObXVQ7X+KZbwXYfmZ0PIzSrwA89BVZ78K5hQgnTJPkSDJvxIlqFbu6qz9hmGrtaD3ixyPoopMgmIzM2Pldk749bWEdt//wunHCrbEB6bIfoc+w8bpASTV8qsdyHlsLowRTNxoGkPsE7Eewo2KkKhumyng21fxR0MJo+vAoGAK/k8t3hkjhICAeBGQ9bu7WTCI3NDSqXKSw39gMONarZl78ykQI8Hx3Jzxz2xpMv+pOADxjDets+tFHjD6DVW+00bIKYBmHV5gh0sELyKRj/S5LJGXPneyMzOz8w08rE+QSAuLIBbjfpbKvpqRVIuZSREY4JQSpBeDK04i4HiyyECgYEA7RWQm5tdoR+lsIaxF36wIQkSqJHNW3g8q3zWzLJYNmPD6uCi0KaryP3t8F018FLVGefu1CiUbb4zRiaVFxRRqIC3P5pgpHpec1Iev6z3v92uXKXt3F7XXKbe6/OrAqFxy9+f9ULn9BCU+lP94U+rMo6bPsZs+qvBHmW7qOrRawo=";
    String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp4UwbuQvJv5+ghbFKGYk/nmqxxiV+J/p8K7mQ9agiMANCgpT0XPuIjRzb6qmqVtlpZDI5QpMhTCN+WG15srJV4M1dQOgt9FgtpbVhNFWibsONWldID+cSKLR019dVGBFotrg2/l0Pq8jftK0hsoa3jscined10knQQ7vdXf1/ngJOcxUpBlcQYQliCzt+jeict17OQrrp3xaFZVF0cgr22c2QKQ0gXYUIR23/Y1vzCAvJtaif22KR6VLnE1ySDCG/em/48nuslWV8nY5zxtEVRZDPnEHL8P6wl4WLGp2TIkRFjTLro5MwBkV2dpOy7h7vgvjpppfozMUTsuh2UYWyQIDAQAB";
    String CHARSET = "utf-8";
    String serverUrl = "https://openapi.alipaydev.com/gateway.do";

    @GetMapping("/alipaytest")
    public void doPost(HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws ServletException, IOException {
        AlipayClient alipayClient = new DefaultAlipayClient(
                serverUrl,
                APP_ID,
                APP_PRIVATE_KEY,
                "json",
                CHARSET,
                ALIPAY_PUBLIC_KEY,
                "RSA2"); //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        alipayRequest.setBizContent("{" +
                " \"out_trade_no\":\"20150320010101005\"," +
                " \"total_amount\":\"88.88\"," +
                " \"subject\":\"Iphone6 16G\"," +
                " \"product_code\":\"QUICK_WAP_WAY\"" +
                " }");//填充业务参数
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    @GetMapping("/selectOrder")
    @ResponseBody
    public String select() throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, APP_ID, APP_PRIVATE_KEY, "json", CHARSET, ALIPAY_PUBLIC_KEY, "RSA2");  //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest(); //创建API对应的request类
        request.setBizContent("{" +
                "    \"out_trade_no\":\"20150320010101004\" "
                + "}");  //设置业务参数
        AlipayTradeQueryResponse response = alipayClient.execute(request); //通过alipayClient调用API，获得对应的response类
//        System.out.print(response.getBody());
        return response.getBody();
    }


    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "aaa";
    }
}
