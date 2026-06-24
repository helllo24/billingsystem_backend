package Billing_Voice_System.service;

import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.ItemDto;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;

@Service
public class PdfBilling_Service {

    public byte[] generatepdfBill(FinalBillDto billDto){


        ByteArrayOutputStream outputStream= new ByteArrayOutputStream(); // Container
        PdfWriter writer = new PdfWriter(outputStream); // content what we to be write
        PdfDocument pdfDocument = new PdfDocument(writer); // main part contains pages, slides
        Document doc = new Document(pdfDocument);

        //title
        doc.add(new Paragraph("BILLING SYSTEM").setBold().setFontSize(38).setBackgroundColor(ColorConstants.CYAN));
        doc.add(new Paragraph("BILL RECEIPT ").setBold().setFontSize(19));


        //table
        float[]  columnwidth = {200f,100f,100f,100f};
        Table tab = new Table(columnwidth);

        //Heading
        tab.addCell("items");
                tab.addCell("Qty");
                tab.addCell("Price");
                tab.addCell("Total");

                //content use loop get datas

        for (ItemDto item : billDto.getItems()){

            tab.addCell(item.getName());
            tab.addCell(String.valueOf(item.getQty()));
            tab.addCell(String.valueOf(item.getPrice()));

            double itemTotal = item.getQty()* item.getPrice();
            tab.addCell(String.valueOf(itemTotal));




        }

        doc.add(tab);

        //total
        doc.add(new Paragraph("Total : ₹ " + billDto.getTotal()).setFontSize(20));

        doc.close();

        return outputStream.toByteArray();








    }







    
}
