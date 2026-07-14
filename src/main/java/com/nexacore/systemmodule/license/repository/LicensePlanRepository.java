package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicensePlanRepository extends JpaRepository<SysLicensePlan, Long> {
    Optional<SysLicensePlan> findByPlanCode(String planCode);
}
