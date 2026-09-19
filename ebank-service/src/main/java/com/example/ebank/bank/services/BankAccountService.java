package com.example.ebank.bank.services;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.model.Customer;
import com.example.ebank.bank.repositories.BankAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRestClient customerRestClient;
    private final CustomerLookupService customerLookupService;

    @McpTool(
            name = "getAllBankAccounts",
            description = "Retrieve the full list of bank accounts registered in EBank Service. Use this when the user asks to list accounts or to find accounts for a given customer by inspecting customerId on each account.")
    public List<BankAccount> findAll() {
        return bankAccountRepository.findAll();
    }

    @McpTool(
            name = "getBankAccountById",
            description = "Retrieve one bank account from EBank Service using the account's unique UUID. Also loads the associated customer (or a fallback if Customer Service is unavailable).")
    public BankAccount findById(
            @McpToolParam(
                    description = "The unique UUID string that identifies the bank account to retrieve",
                    required = true)
            String id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with id " + id));
        Customer customer = customerLookupService.getCustomerById(bankAccount.getCustomerId());
        bankAccount.setCustomer(customer);
        return bankAccount;
    }

    @McpTool(
            name = "saveBankAccount",
            description = "Create or save a bank account in EBank Service. The customerId must already exist in Customer Service. Account type must be CURRENT or SAVING.")
    public BankAccount save(
            @McpToolParam(description = "The bank account to create or persist. Include customerId (existing customer), balance (account amount), type (CURRENT or SAVING), and optionally createdAt. The id is generated as a UUID.")
            BankAccount bankAccount) {
        Customer customer = customerRestClient.getCustomerById(bankAccount.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("Customer not found with id " + bankAccount.getCustomerId());
        }
        BankAccount saved = bankAccountRepository.save(bankAccount);
        saved.setCustomer(customer);
        return saved;
    }
}
