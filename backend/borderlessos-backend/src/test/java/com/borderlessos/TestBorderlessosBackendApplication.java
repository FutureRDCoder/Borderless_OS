package com.borderlessos;

import org.springframework.boot.SpringApplication;

public class TestBorderlessosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(BorderlessosBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
