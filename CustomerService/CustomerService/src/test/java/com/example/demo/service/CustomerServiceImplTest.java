package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.Customer;
import com.example.demo.repository.CustomerRepository;

class CustomerServiceImplTest {

    @Test
    void returnsAllCustomersFromRepository() {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer first = new Customer();
        Customer second = new Customer();
        when(repository.findAllByOrderByCustomerIdDesc()).thenReturn(List.of(first, second));
        CustomerServiceImpl service = new CustomerServiceImpl(repository, mock(RestTemplate.class));

        assertEquals(List.of(first, second), service.getCustomers());
        verify(repository).findAllByOrderByCustomerIdDesc();
    }
}
