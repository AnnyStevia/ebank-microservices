package com.example.ebank.bank.services;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.model.Customer;
import com.example.ebank.bank.repositories.BankAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRestClient customerRestClient;
    private final CustomerLookupService customerLookupService;

    public List<BankAccount> findAll() {
        return bankAccountRepository.findAll();
    }

    public BankAccount findById(String id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with id " + id));
        Customer customer = customerLookupService.getCustomerById(bankAccount.getCustomerId());
        bankAccount.setCustomer(customer);
        return bankAccount;
    }

    public BankAccount save(BankAccount bankAccount) {
        Customer customer = customerRestClient.getCustomerById(bankAccount.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("Customer not found with id " + bankAccount.getCustomerId());
        }
        BankAccount saved = bankAccountRepository.save(bankAccount);
        saved.setCustomer(customer);
        return saved;
    }
}
