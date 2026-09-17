package com.example.ebank.bank.controllers;

import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.services.BankAccountService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class BankAccountRestController {

    private final BankAccountService bankAccountService;

    @GetMapping
    public List<BankAccount> getAllAccounts() {
        return bankAccountService.findAll();
    }

    @GetMapping("/{id}")
    public BankAccount getAccountById(@PathVariable String id) {
        return bankAccountService.findById(id);
    }

    @PostMapping
    public BankAccount saveAccount(@RequestBody BankAccount bankAccount) {
        return bankAccountService.save(bankAccount);
    }
}
