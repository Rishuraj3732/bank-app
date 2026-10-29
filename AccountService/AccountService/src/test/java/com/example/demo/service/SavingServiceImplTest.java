package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.demo.entity.Savings;
import com.example.demo.repository.SavingsRepository;

class SavingServiceImplTest {

    @Test
    void returnsAllSavingsAccountsFromRepository() {
        SavingsRepository repository = mock(SavingsRepository.class);
        Savings first = new Savings();
        Savings second = new Savings();
        when(repository.findAllByOrderByAccountnoDesc()).thenReturn(List.of(first, second));
        SavingServiceImpl service = new SavingServiceImpl(repository);

        assertEquals(List.of(first, second), service.getSavingsAccounts());
        verify(repository).findAllByOrderByAccountnoDesc();
    }
}
