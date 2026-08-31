package com.gk.jobhelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 公考智能选岗助手启动类
 */
@SpringBootApplication
@EnableScheduling
public class JobHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobHelperApplication.class, args);
    }
}
