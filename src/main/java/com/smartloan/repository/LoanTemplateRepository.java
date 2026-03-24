package com.smartloan.repository;

import com.smartloan.entity.LoanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanTemplateRepository extends JpaRepository<LoanTemplate, String> {
    List<LoanTemplate> findByUserId(String userId);
}
