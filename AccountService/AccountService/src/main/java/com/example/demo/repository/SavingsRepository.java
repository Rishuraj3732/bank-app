package com.example.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Savings;

import java.util.List;
import java.util.Optional;

public interface SavingsRepository
        extends JpaRepository<Savings, Long> {

    Optional<Savings> findByCustomerId(Long customerId);

    List<Savings> findAllByOrderByAccountnoDesc();

    Optional<Savings> findFirstByOrderByAccountnoAsc();
}
