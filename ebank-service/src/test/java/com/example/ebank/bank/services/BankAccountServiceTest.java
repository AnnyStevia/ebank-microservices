package com.example.ebank.bank.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ebank.bank.entities.BankAccount;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BankAccountServiceTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Test
    void findAllReturnsSeededAccounts() {
        assertThat(bankAccountService.findAll()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void saveAndFindById() {
        BankAccount saved = bankAccountService.save(BankAccount.builder()
                .createdAt(LocalDateTime.now())
                .balance(2500.00)
                .type("CURRENT")
                .customerId(2L)
                .build());

        BankAccount found = bankAccountService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getBalance()).isEqualTo(2500.00);
        assertThat(found.getType()).isEqualTo("CURRENT");
        assertThat(found.getCustomerId()).isEqualTo(2L);
    }

    @Test
    void findByIdThrowsWhenAccountDoesNotExist() {
        assertThatThrownBy(() -> bankAccountService.findById("missing-account-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bank account not found with id missing-account-id");
    }
}
