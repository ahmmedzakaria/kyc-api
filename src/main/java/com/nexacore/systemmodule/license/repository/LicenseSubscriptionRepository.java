package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LicenseSubscriptionRepository extends JpaRepository<SysLicenseSubscription, Long> {
    Optional<SysLicenseSubscription> findBySubscriptionCode(String subscriptionCode);

    @Query("""
            select subscription
            from SysLicenseSubscription subscription
            join fetch subscription.licensePlan plan
            where subscription.status in :statuses
              and (
                    (:businessId is not null and subscription.businessId = :businessId)
                 or (:tenantId is not null and subscription.tenantId = :tenantId)
                 or (:clientApplicationId is not null and subscription.clientApplication.id = :clientApplicationId)
              )
            order by
              case when subscription.businessId = :businessId then 0
                   when subscription.clientApplication.id = :clientApplicationId then 1
                   when subscription.tenantId = :tenantId then 2
                   else 3
              end,
              subscription.id desc
            """)
    List<SysLicenseSubscription> findDecisionCandidates(Long tenantId,
                                                        Long businessId,
                                                        Long clientApplicationId,
                                                        Collection<LicenseStatus> statuses);
}
