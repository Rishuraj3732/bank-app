package com.example.demo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.entity.Customer;
import com.example.demo.service.CustomerService;

class CustomerControllerTest {

    @Test
    void returnsCustomerCollectionFromCollectionRoute() throws Exception {
        Customer customer = new Customer();
        customer.setCustomerId(42L);
        customer.setName("Riya Sharma");
        CustomerService service = mock(CustomerService.class);
        when(service.getCustomers()).thenReturn(List.of(customer));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(service)).build();

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(42))
                .andExpect(jsonPath("$[0].name").value("Riya Sharma"));
    }
}
