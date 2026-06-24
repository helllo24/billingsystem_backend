package Billing_Voice_System.dto;

import ch.qos.logback.core.model.processor.AllowAllModelFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {

    //It Represent billing output

    private String name;
    private double qty;
    private double price;
    private String unit;
    private double totalprice;



}
