package com.example.ebank.customer.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ebank.customer.entities.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void findAllReturnsSeededCustomers() {
        assertThat(customerService.findAll()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void saveAndFindById() {
        Customer saved = customerService.save(Customer.builder()
                .name("Sara Naji")
                .email("sara.naji@example.com")
                .build());

        Customer found = customerService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Sara Naji");
        assertThat(found.getEmail()).isEqualTo("sara.naji@example.com");
    }

    @Test
    void findByIdThrowsWhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> customerService.findById(999_999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found with id 999999");
    }
}
