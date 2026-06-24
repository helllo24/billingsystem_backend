package Billing_Voice_System.repository;

import Billing_Voice_System.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingItemRepository extends JpaRepository<BillItem, Long> {


    List<BillItem> findByBill_Billno(Long billno);
}
