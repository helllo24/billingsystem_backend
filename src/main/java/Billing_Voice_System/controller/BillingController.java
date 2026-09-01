package Billing_Voice_System.controller;

import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.RawDto;
import Billing_Voice_System.service.BillService;
import Billing_Voice_System.service.Billing_SpeechService;
import Billing_Voice_System.service.PdfBilling_Service;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import Billing_Voice_System.entity.BillRaw;
import java.util.List;
import java.io.IOException;

@RestController


@RequestMapping("Bill")

public class BillingController {

    private final BillService billService;
    private final Billing_SpeechService billingSpeechService;
    private final PdfBilling_Service pdfBillingService;

    public BillingController(BillService billService, Billing_SpeechService billingSpeechService, PdfBilling_Service pdfBillingService) {
        this.billService = billService;
        this.billingSpeechService = billingSpeechService;
        this.pdfBillingService = pdfBillingService;
    }


    @PostMapping("/askBill")
    public ResponseEntity<FinalBillDto> generateBill(@RequestBody RawDto rawDto) {
        FinalBillDto finalBillDto = billService.generateBil(rawDto);

        return ResponseEntity.ok(finalBillDto);
    }

    //new
    @PostMapping("/askvoiceBill")
    public ResponseEntity<FinalBillDto> generateVoiceApi(@RequestParam("file")MultipartFile file) throws IOException {

        //
        String text = billingSpeechService.convertAudioToText(file.getBytes());
        System.out.println("VOICE TEXT: " + text);
        // Step 2: Wrap that text in a RawDto
        RawDto rawDto = new RawDto();
        rawDto.setText(text);

        // Step 3: Reuse your existing text service logic
        // This will automatically parse the text via AI and save it to the DB
        return ResponseEntity.ok(billService.generateBil(rawDto));


    }
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<FinalBillDto> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillid(id));
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> getpdf(@PathVariable Long id){

        FinalBillDto bill = billService.getBillid(id);

                byte[]  pdf = pdfBillingService.generatepdfBill(bill);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=bill.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);

    }

    @GetMapping
    public ResponseEntity<List<BillRaw>> getAllInvoices() {
        return ResponseEntity.ok(billService.getAllInvoices());
    }

    @PostMapping("/upload-audio")
    public ResponseEntity<FinalBillDto> uploadAudioAndGenerateBill(@RequestParam("file") MultipartFile file) throws IOException {
        String savedFilename = billService.saveAudioFile(file);
        System.out.println("Audio file saved successfully: " + savedFilename);

        String text = billingSpeechService.convertAudioToText(file.getBytes());
        System.out.println("VOICE TEXT FROM UPLOADED AUDIO: " + text);

        RawDto rawDto = new RawDto();
        rawDto.setText(text);

        return ResponseEntity.ok(billService.generateBil(rawDto));
    }
}
