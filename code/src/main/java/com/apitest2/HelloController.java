package com.apitest2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${app.api.AccuW}")
    private String AccuWApiKey;

    @RequestMapping("/hello")
    public String hello() {
        return AccuWApiKey;
    }    
    
    @RequestMapping("/hello2")
    public String hello2() {
        return "hello twin";
    }   

    @RequestMapping("/hello3")
    public String hello3() {
        return "hello xd lol";
    }   
}
