package com.isoplatform.api.certification.repository;

import com.isoplatform.api.certification.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByVin(String vin);
    Optional<Certificate> findByCertNumber(String certNumber);
    boolean existsByVin(String vin);
}
