package com.example.demo.service;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.csp.operationlog.aspect.annotation.OperationLog;
import com.csp.operationlog.aspect.enums.OperationType;
import com.csp.operationlog.service.OperationLogService;
import com.example.demo.domain.Account;
import com.example.demo.mapper.AccountMapper;
@Service
public class AccountService {
    @Resource
    AccountMapper accountMapper;
    @Resource
    OperationLogService operationLogService;

    //采用注解方式,实现操作日志的记录,适用于大多数简单服务,不涉及代码中多表更改的业务
    @OperationLog(name = "添加账户",type = OperationType.ADD,operatorIdRef = 0,operatorNameRef = 1,table = "account")
    public void addAccount(String operatorId,String operatorName){
        Account account = new Account();
        account.setAccount(181);
        accountMapper.insertAccount(account);
    }

    @OperationLog(name = "更新账户",type = OperationType.UPDATE,operatorIdRef = 0,operatorNameRef = 1,idRef = 2,table = "account")
    public void updateAccount(String operatorId,String operatorName,Integer accountId){
        Account account = new Account();
        account.setId(accountId);
        account.setAccount(1100);
        accountMapper.updateAccount(account);
    }

    @OperationLog(name = "删除账户",type = OperationType.DELETE,operatorIdRef = 0,operatorNameRef = 1,idRef = 2,table = "account")
    public void deleteAccount(String operatorId,String operatorName,Integer accountId){
        accountMapper.deleteAccountById(accountId);
    }

    //使用服务调用的方式,实现操作日志的记录,为了在注解无法解决业务代码中对多个表操作时的应对方法
    public void addAcccount2(){
        Account account=new Account();
        account.setAccount(181);
        accountMapper.insertAccount(account);
        operationLogService.logForAdd("添加账户","account","1","liutao",account);
    }

    public void updateAccount2(){
        Account account = accountMapper.selectAccountById(1);
        if (account!=null){
            Account accountOld = account.clone();
            account.setId(1);
            account.setAccount(0);
            accountMapper.updateAccount(account);
            operationLogService.logForUpd("更新账户","account",account.getId().toString(),"1","liutao",accountOld,account);
        }
    }

    public void deleteAccount2(){
        Account account = accountMapper.selectAccountById(1);
        if (account!=null){
            accountMapper.deleteAccountById(1);
            operationLogService.logForDel("删除账户","account",account.getId().toString(),"1","liutao",account);
        }
    }

}
