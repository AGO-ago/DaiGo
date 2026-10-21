package org.com.first.zhao_waimai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.com.first.zhao_waimai.mapper")
public class ZhaoWaimaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhaoWaimaiApplication.class, args);
    }

}
