package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
//depricated
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}