package com.denzel.mpesa.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByCheckoutRequestId(String checkoutRequestId);

    List<Transaction> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);
}
