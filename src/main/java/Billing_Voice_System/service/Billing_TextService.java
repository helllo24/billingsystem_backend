package Billing_Voice_System.service;

import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.ItemDto;
import Billing_Voice_System.dto.RawDto;
import Billing_Voice_System.entity.BillItem;
import Billing_Voice_System.entity.BillRaw;
import Billing_Voice_System.repository.BillingItemRepository;
import Billing_Voice_System.repository.BillingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Billing_TextService implements BillService {

    private final Billing_Ai_ParseService billingAiParseService;
    private final BillingRepository billrepo;
    private final BillingItemRepository billingItemRepository;

    public Billing_TextService(Billing_Ai_ParseService billingAiParseService, BillingRepository billrepo, BillingItemRepository billingItemRepository) {
        this.billingAiParseService = billingAiParseService;
        this.billrepo = billrepo;
        this.billingItemRepository = billingItemRepository;
    }


    @Override
    public FinalBillDto generateBil(RawDto rawDto) {

        //get input
        String input = rawDto.getText();

        //call AI method for (text -> json -> Dto )
        FinalBillDto responce = billingAiParseService.parerText(input);

        //Recalculate Amount
//        double total = calculateTotal(responce.getItems());
//        responce.setTotal(total);

        //save Raw value in Db
        BillRaw billRaw = new BillRaw();
        billRaw.setRawtext(input);

        billRaw.setTotal(responce.getTotal());

        BillRaw savedBill  = billrepo.save(billRaw);

        for(ItemDto dto : responce.getItems()) {

            BillItem item = new BillItem();

            item.setName(dto.getName());
            item.setQty(dto.getQty());
            item.setPrice(dto.getPrice());
            item.setTotalprice(dto.getTotalprice());
            item.setUnit(dto.getUnit());

            item.setBill(savedBill);

            billingItemRepository.save(item);
        }

        return  responce;

    }

    @Override
    public FinalBillDto getBillid(Long id) {
        BillRaw billRaw = billrepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Bill not found"));

        List<BillItem> billItems =
                billingItemRepository.findByBill_Billno(id);

        List<ItemDto> itemDtos = new ArrayList<>();

        for (BillItem item : billItems) {

            ItemDto dto = new ItemDto();

            dto.setName(item.getName());
            dto.setQty(item.getQty());
            dto.setPrice(item.getPrice());
            dto.setTotalprice(item.getTotalprice());
            dto.setUnit(item.getUnit());

            itemDtos.add(dto);
        }

        FinalBillDto finalDto = new FinalBillDto();

        finalDto.setItems(itemDtos);
        finalDto.setTotal(billRaw.getTotal());

        return finalDto;


    }

    private double calculateTotal(List<ItemDto> items){

        return items.stream()
                .mapToDouble(item -> item.getQty() * item.getPrice())
                .sum();
    }
}
