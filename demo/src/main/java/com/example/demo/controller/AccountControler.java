package com.example.demo.controller;

import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.demo.service.AccountService;
@Controller
public class AccountControler {
    @Resource
    private AccountService accountService;
    //采用注解方式
    @RequestMapping("/t1")
    @ResponseBody
    public String addTest(){
        accountService.addAccount("1","liutao");
        return "ok";
    }
    @RequestMapping("/t2")
    @ResponseBody
    public String updateTest(){
        accountService.updateAccount("1","liutao",1);
        return "ok";
    }
    @RequestMapping("/t3")
    @ResponseBody
    public String deleteTest(){
        accountService.deleteAccount("1","liutao",1);
        return "ok";
    }

    //采用服务调用
    @RequestMapping("/s1")
    @ResponseBody
    public String addTest2(){
        accountService.addAcccount2();
        return "ok";
    }
    @RequestMapping("/s2")
    @ResponseBody
    public String updateTest2() {
        accountService.updateAccount2();
        return "ok";
    }
    @RequestMapping("/s3")
    @ResponseBody
    public String deleteTest2(){
        accountService.deleteAccount2();
        return "ok";
    }
}
