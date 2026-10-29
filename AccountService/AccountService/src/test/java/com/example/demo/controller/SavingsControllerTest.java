package com.example.demo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.entity.Savings;
import com.example.demo.service.SavingsService;

class SavingsControllerTest {

    @Test
    void returnsSavingsCollectionFromCollectionRoute() throws Exception {
        Savings account = new Savings();
        account.setAccountno(101L);
        account.setCustomerId(42L);
        account.setOpeningDate(LocalDate.of(2026, 10, 29));
        account.setBalance(2500.0);
        SavingsService service = mock(SavingsService.class);
        when(service.getSavingsAccounts()).thenReturn(List.of(account));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SavingsController(service)).build();

        mockMvc.perform(get("/api/v1/savings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountno").value(101))
                .andExpect(jsonPath("$[0].customerId").value(42))
                .andExpect(jsonPath("$[0].balance").value(2500.0));
    }
}
