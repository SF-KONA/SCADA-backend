package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<Partner, String> {
}