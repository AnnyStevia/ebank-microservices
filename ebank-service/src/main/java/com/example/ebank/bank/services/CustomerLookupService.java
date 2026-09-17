package com.example.ebank.bank.services;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.model.Customer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerLookupService {

    private final CustomerRestClient customerRestClient;

    @CircuitBreaker(name = "customerService", fallbackMethod = "getDefaultCustomer")
    public Customer getCustomerById(Long id) {
        return customerRestClient.getCustomerById(id);
    }

    public Customer getDefaultCustomer(Long id, Throwable throwable) {
        return Customer.builder()
                .id(id)
                .name("Not Available")
                .email("not available")
                .build();
    }
}
