package com.example.demo.service;

import com.example.demo.dto.SavingsRequest;
import com.example.demo.entity.Savings;

import java.util.List;

public interface SavingsService {

    Savings createSavings(SavingsRequest request);

    List<Savings> getSavingsAccounts();

    Savings getSavingsByCustomerId(Long customerId);

    Savings getProfile();
}
