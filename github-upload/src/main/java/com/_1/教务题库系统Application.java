package com._1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class 教务题库系统Application {

	public static void main(String[] args) {
		SpringApplication.run(教务题库系统Application.class, args);
	}

}
