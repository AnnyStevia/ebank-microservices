package com.example.ebank.bank.controllers;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.model.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BankAccountRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getAllAccountsReturnsSeededData() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));
    }

    @Test
    void postAccountThenGetById() throws Exception {
        String payload = """
                {
                  "createdAt": "2026-09-17T10:00:00",
                  "balance": 3200.25,
                  "type": "SAVING",
                  "customerId": 3
                }
                """;

        MvcResult created = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.balance").value(3200.25))
                .andExpect(jsonPath("$.type").value("SAVING"))
                .andExpect(jsonPath("$.customerId").value(3))
                .andExpect(jsonPath("$.customer.id").value(3))
                .andExpect(jsonPath("$.customer.name").value("Test Customer"))
                .andReturn();

        BankAccount saved = objectMapper.readValue(created.getResponse().getContentAsString(), BankAccount.class);

        mockMvc.perform(get("/accounts/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.type").value("SAVING"))
                .andExpect(jsonPath("$.customer.id").value(3))
                .andExpect(jsonPath("$.customer.name").value("Test Customer"))
                .andExpect(jsonPath("$.customer.email").value("test@example.com"));
    }

    @Test
    void getAccountByIdReturnsFallbackCustomerWhenCustomerServiceFails() throws Exception {
        String payload = """
                {
                  "createdAt": "2026-09-17T10:00:00",
                  "balance": 1100.00,
                  "type": "CURRENT",
                  "customerId": 2
                }
                """;

        MvcResult created = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        BankAccount saved = objectMapper.readValue(created.getResponse().getContentAsString(), BankAccount.class);

        when(customerRestClient.getCustomerById(2L))
                .thenThrow(new RuntimeException("Customer Service unavailable"));

        mockMvc.perform(get("/accounts/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.customer.id").value(2))
                .andExpect(jsonPath("$.customer.name").value("Not Available"))
                .andExpect(jsonPath("$.customer.email").value("not available"));
    }
}
