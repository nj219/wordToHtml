package com.wordtohtml.wordtohtml.controller;

import com.wordtohtml.wordtohtml.util.HttpClientUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * @ClassName saveTest
 * @Author shangpinyuan06
 * @Date 2019/8/26 18:50
 **/
@Controller
@RequestMapping("hhh")
@CrossOrigin
public class saveTest {

    @PostMapping("save")
    public void save(String imgBase64) {
        MultipartFile multipartFile = BASE64DecodedMultipartFile.base64ToMultipart(imgBase64);
        HttpClientUtils.httpClientUploadFile("http://localhost:8080/wup/webUploader?myPath=bfcec", multipartFile);
        System.out.println();
    }
}
