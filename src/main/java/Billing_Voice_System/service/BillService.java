package Billing_Voice_System.service;

import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.RawDto;

public interface BillService {


    FinalBillDto generateBil (RawDto rawDto);


    FinalBillDto getBillid (Long id);



}
