package org.duqiu.fly.autoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.duqiu.fly.autoapi")
public class AutoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoApiApplication.class, args);
    }

}