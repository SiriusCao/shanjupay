package com.shanjupay.merchant;

import com.shanjupay.common.util.QiniuUtils;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QiniuTest {
    @Test
    public void test1(){
//        QiniuUtils.upload2Qiniu("C:\\Users\\schwarzenegger\\Pictures\\Camera Roll\\IMG_20210607_095616_BURST005.jpg","66666");
        String fileName="abcd.txt";
        System.out.println(fileName.substring(fileName.lastIndexOf(".")-1));
    }
}
