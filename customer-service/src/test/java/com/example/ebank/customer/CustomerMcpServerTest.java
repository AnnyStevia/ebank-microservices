package com.example.ebank.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ebank.customer.entities.Customer;
import com.example.ebank.customer.services.CustomerService;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerMcpServerTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private McpSyncServer mcpSyncServer;

    @Autowired
    private McpServerProperties mcpServerProperties;

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint;

    @Test
    void contextStartsWithStreamableMcpServer() {
        assertThat(mcpSyncServer).isNotNull();
        assertThat(mcpServerProperties.getProtocol()).isEqualTo(McpServerProperties.ServerProtocol.STREAMABLE);
        assertThat(mcpServerProperties.getType()).isEqualTo(McpServerProperties.ApiType.SYNC);
        assertThat(mcpServerProperties.getName()).isEqualTo("customer-service");
        assertThat(mcpEndpoint).isEqualTo("/mcp");
    }

    @Test
    void existingCustomerOperationsStillWork() {
        assertThat(customerService.findAll()).hasSizeGreaterThanOrEqualTo(3);

        Customer saved = customerService.save(Customer.builder()
                .name("Nadia El Fassi")
                .email("nadia.elfassi@example.com")
                .build());

        Customer found = customerService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Nadia El Fassi");
        assertThat(found.getEmail()).isEqualTo("nadia.elfassi@example.com");
    }

    @Test
    void registeredMcpToolsExposeCustomerOperations() {
        List<String> toolNames = mcpSyncServer.listTools().stream()
                .map(McpSchema.Tool::name)
                .toList();

        assertThat(toolNames).contains("getAllCustomers", "getCustomerById", "saveCustomer");
    }
}
