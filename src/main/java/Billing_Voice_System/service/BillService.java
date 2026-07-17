package Billing_Voice_System.service;

import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.RawDto;

import Billing_Voice_System.entity.BillRaw;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BillService {


    FinalBillDto generateBil (RawDto rawDto);


    FinalBillDto getBillid (Long id);

    List<BillRaw> getAllInvoices();

    String saveAudioFile(MultipartFile file);

}
