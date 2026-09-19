package com.example.ebank.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.example.ebank.bank.clients.CustomerRestClient;
import com.example.ebank.bank.entities.BankAccount;
import com.example.ebank.bank.model.Customer;
import com.example.ebank.bank.services.BankAccountService;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BankAccountMcpServerTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private McpSyncServer mcpSyncServer;

    @Autowired
    private McpServerProperties mcpServerProperties;

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint;

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
    void contextStartsWithStreamableMcpServer() {
        assertThat(mcpSyncServer).isNotNull();
        assertThat(mcpServerProperties.getProtocol()).isEqualTo(McpServerProperties.ServerProtocol.STREAMABLE);
        assertThat(mcpServerProperties.getType()).isEqualTo(McpServerProperties.ApiType.SYNC);
        assertThat(mcpServerProperties.getName()).isEqualTo("ebank-service");
        assertThat(mcpEndpoint).isEqualTo("/mcp");
    }

    @Test
    void existingAccountOperationsStillWork() {
        assertThat(bankAccountService.findAll()).hasSizeGreaterThanOrEqualTo(4);

        BankAccount saved = bankAccountService.save(BankAccount.builder()
                .createdAt(LocalDateTime.now())
                .balance(2100.00)
                .type("CURRENT")
                .customerId(1L)
                .build());

        BankAccount found = bankAccountService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getBalance()).isEqualTo(2100.00);
        assertThat(found.getType()).isEqualTo("CURRENT");
        assertThat(found.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void registeredMcpToolsExposeBankAccountOperations() {
        List<String> toolNames = mcpSyncServer.listTools().stream()
                .map(McpSchema.Tool::name)
                .toList();

        assertThat(toolNames).contains("getAllBankAccounts", "getBankAccountById", "saveBankAccount");
    }
}
