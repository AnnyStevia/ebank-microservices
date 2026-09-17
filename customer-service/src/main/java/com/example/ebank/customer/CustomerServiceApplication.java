package com.example.ebank.customer;

import com.example.ebank.customer.entities.Customer;
import com.example.ebank.customer.repositories.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner sampleData(CustomerRepository customerRepository) {
        return args -> {
            customerRepository.save(Customer.builder()
                    .name("Hassan El Amrani")
                    .email("hassan.elamrani@example.com")
                    .build());
            customerRepository.save(Customer.builder()
                    .name("Amina Benali")
                    .email("amina.benali@example.com")
                    .build());
            customerRepository.save(Customer.builder()
                    .name("Youssef Kadiri")
                    .email("youssef.kadiri@example.com")
                    .build());
        };
    }
}
