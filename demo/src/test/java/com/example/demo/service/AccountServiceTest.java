package com.example.demo.service;

import com.example.demo.DemoApplication;
import com.example.demo.DemoApplicationTests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class)
public class AccountServiceTest {
    @Autowired
    AccountService  accountService;

    @Test
    public void addAccount() {
        accountService.addAccount("1","litao");
    }

    @Test
    public void updateAccount() {
        accountService.updateAccount("1","liutao",1);
    }

    @Test
    public void deleteAccount() {
        accountService.deleteAccount("1","liutao",1);
    }
}