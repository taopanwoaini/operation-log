package com.example.demo.mapper;

import com.example.demo.domain.Account;
import org.apache.ibatis.annotations.*;
@Mapper
public interface AccountMapper {
    @Insert("INSERT INTO account (account) VALUES (#{a.account})")
    public void insertAccount(@Param("a") Account a);
    @Update("UPDATE account SET account=#{a.account} WHERE id=#{a.id}")
    public void updateAccount(@Param("a") Account a);
    @Select("DELETE FROM account WHERE id=#{id}")
    public void deleteAccountById(@Param("id") Integer id);
    @Select("SELECT id,account FROM account WHERE id=#{id}")
    public Account selectAccountById(@Param("id")Integer id);
}
