package com.example.ebank.customer.services;

import com.example.ebank.customer.entities.Customer;
import com.example.ebank.customer.repositories.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @McpTool(
            name = "getAllCustomers",
            description = "Retrieve the full list of customers registered in Customer Service")
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @McpTool(
            name = "getCustomerById",
            description = "Retrieve one customer from Customer Service using the customer's unique ID")
    public Customer findById(
            @McpToolParam(
                    description = "The unique identifier of the customer to retrieve",
                    required = true)
            Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id " + id));
    }

    @McpTool(
            name = "saveCustomer",
            description = "Create or save a customer in Customer Service")
    public Customer save(
            @McpToolParam(description = "The customer to create or persist, including name and email")
            Customer customer) {
        return customerRepository.save(customer);
    }
}
