package com.example.ebank.bank.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.model.Customer;
import com.example.ebank.bank.repositories.BankAccountRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BankAccountServiceTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @MockitoBean
    private CustomerRestClient customerRestClient;

    @BeforeEach
    void stubCustomerClient() {
        when(customerRestClient.getCustomerById(anyLong()))
                .thenAnswer(invocation -> Customer.builder()
                        .id(invocation.getArgument(0))
                        .name("Test Customer")
                        .email("test@example.com")
                        .build());
    }

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

        assertThat(saved.getCustomer()).isNotNull();
        assertThat(saved.getCustomer().getId()).isEqualTo(2L);

        BankAccount found = bankAccountService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getBalance()).isEqualTo(2500.00);
        assertThat(found.getType()).isEqualTo("CURRENT");
        assertThat(found.getCustomerId()).isEqualTo(2L);
        assertThat(found.getCustomer()).isNotNull();
        assertThat(found.getCustomer().getId()).isEqualTo(2L);
        assertThat(found.getCustomer().getName()).isEqualTo("Test Customer");
    }

    @Test
    void findByIdThrowsWhenAccountDoesNotExist() {
        assertThatThrownBy(() -> bankAccountService.findById("missing-account-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bank account not found with id missing-account-id");
    }

    @Test
    void saveSucceedsWhenCustomerExists() {
        BankAccount saved = bankAccountService.save(BankAccount.builder()
                .createdAt(LocalDateTime.now())
                .balance(1800.00)
                .type("SAVING")
                .customerId(1L)
                .build());

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCustomerId()).isEqualTo(1L);
        assertThat(saved.getCustomer().getId()).isEqualTo(1L);
        assertThat(bankAccountRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void saveThrowsWhenCustomerDoesNotExist() {
        when(customerRestClient.getCustomerById(999L))
                .thenThrow(new RuntimeException("Customer not found with id 999"));

        long countBefore = bankAccountRepository.count();

        assertThatThrownBy(() -> bankAccountService.save(BankAccount.builder()
                .createdAt(LocalDateTime.now())
                .balance(100.00)
                .type("CURRENT")
                .customerId(999L)
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found with id 999");

        assertThat(bankAccountRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void findByIdReturnsFallbackCustomerWhenCustomerServiceFails() {
        BankAccount saved = bankAccountService.save(BankAccount.builder()
                .createdAt(LocalDateTime.now())
                .balance(900.00)
                .type("CURRENT")
                .customerId(1L)
                .build());

        when(customerRestClient.getCustomerById(1L))
                .thenThrow(new RuntimeException("Customer Service unavailable"));

        BankAccount found = bankAccountService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getCustomerId()).isEqualTo(1L);
        assertThat(found.getCustomer()).isNotNull();
        assertThat(found.getCustomer().getId()).isEqualTo(1L);
        assertThat(found.getCustomer().getName()).isEqualTo("Not Available");
        assertThat(found.getCustomer().getEmail()).isEqualTo("not available");
    }
}
