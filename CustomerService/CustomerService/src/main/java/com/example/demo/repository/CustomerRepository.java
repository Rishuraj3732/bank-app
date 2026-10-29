package com.example.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    List<Customer> findAllByOrderByCustomerIdDesc();

    Optional<Customer> findFirstByOrderByCustomerIdAsc();
}
