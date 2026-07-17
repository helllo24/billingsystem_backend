package Billing_Voice_System.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinalBillDto {

    private Long billno;
    private List<ItemDto> items;
    private double total;


}
