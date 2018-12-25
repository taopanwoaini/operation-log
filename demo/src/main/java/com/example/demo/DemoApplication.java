package com.example.demo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
@SpringBootApplication
@ComponentScan(basePackages = {"com.csp.**","com.example.**"})//这里是项目对SpringBean注入的扫描,前面是对operationlog项目中bean的扫描,后面是demo项目的bean的扫描
@MapperScan({"com.csp.operationlog.mapper","com.example.demo.**.mapper"})//这里com.csp.**是扫描我的operationlog项目的mapper,而com.example.**扫描的是我的demo项目的mapper
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

