package Billing_Voice_System.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_raw")
public class BillRaw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billno;

    private String rawtext;

    private Double total;


    private LocalDateTime billat;



    @PrePersist
    public void oncreate(){
        this.billat = LocalDateTime.now();



    }
}
