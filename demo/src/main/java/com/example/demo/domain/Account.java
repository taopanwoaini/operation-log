package com.example.demo.domain;
import java.io.Serializable;
public class Account implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer id;
    private Integer account;

    public Account clone() {
        try {
            Account proto = (Account) super.clone();
            return proto;
        }catch (CloneNotSupportedException e){
            return null;
        }
    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getAccount() {
        return account;
    }
    public void setAccount(Integer account) {
        this.account = account;
    }
}
