package Billing_Voice_System.repository;

import Billing_Voice_System.entity.BillRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingRepository extends JpaRepository<BillRaw,Long> {







}
