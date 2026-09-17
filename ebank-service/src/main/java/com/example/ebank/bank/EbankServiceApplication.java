package com.example.ebank.bank;

import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.repositories.BankAccountRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class EbankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbankServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner sampleData(BankAccountRepository bankAccountRepository) {
        return args -> {
            LocalDateTime now = LocalDateTime.now();
            bankAccountRepository.save(BankAccount.builder()
                    .createdAt(now)
                    .balance(12000.50)
                    .type("CURRENT")
                    .customerId(1L)
                    .build());
            bankAccountRepository.save(BankAccount.builder()
                    .createdAt(now)
                    .balance(8500.00)
                    .type("SAVING")
                    .customerId(1L)
                    .build());
            bankAccountRepository.save(BankAccount.builder()
                    .createdAt(now)
                    .balance(4300.75)
                    .type("CURRENT")
                    .customerId(2L)
                    .build());
            bankAccountRepository.save(BankAccount.builder()
                    .createdAt(now)
                    .balance(15750.20)
                    .type("SAVING")
                    .customerId(3L)
                    .build());
        };
    }
}
