package Billing_Voice_System.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Double qty;

    private Double price;

    private Double totalprice;

    private String unit;

    @ManyToOne
    @JoinColumn(name = "bill_no")
    private BillRaw bill;




}
