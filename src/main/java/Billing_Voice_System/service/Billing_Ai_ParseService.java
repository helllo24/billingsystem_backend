package Billing_Voice_System.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import Billing_Voice_System.dto.FinalBillDto;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Billing_Ai_ParseService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // Use v1beta and ensure the colon (:) is before generateContent
    private static final String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";


    public FinalBillDto parerText(String input) {

        String prompt = """
You are a billing assistant.

Convert the given text into a structured JSON bill.

STRICT RULES:
1. Return ONLY valid JSON
2. Extract: name, qty, price, totalPrice, unit
3. qty must be a number
4. unit must be one of: kg, litre, piece (default = piece)

PRICE RULE:
- price = per unit price
- totalPrice = total price for that item



EXAMPLES:
- "1 kg apple 100 rs" → price = 100, totalPrice = 100
- "5 kg onion 100 rs" → totalPrice = 100, price = 100 / 5 = 20

OTHER RULES:
5. If qty missing → qty = 1
6. If price missing → price = 0
7. If total not given → totalPrice = qty × price
8. Support Tamil & English

IMPORTANT:
- total = sum of all totalPrice



INPUT:
"%s"

OUTPUT FORMAT:
{
  "items": [
    {
      "name": "string",
      "qty": number,
      "price": number,
      "totalprice": number,
      "unit": "kg/litre/piece"
    }
  ],
  "total": number
}
""".formatted(input);


        //set restemplete for communicate extranal
        RestTemplate restTemplate = new RestTemplate();

        //headers
        //gemini no needs authorization  so onlu need content type , because it tell
        // how data to be must like JSON format
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //nested box ( Russian doll method ) noting but inside - > inside ->
        //like a box
        Map<String,Object> box1 = new HashMap<>();
        box1.put("text",prompt);

        //that box inside the another one
        Map<String,Object> box2 = new HashMap<>();
        box2.put("parts", List.of(box1));

        //finally create a body ( containner )
        Map<String , Object> body = new HashMap<>();
        body.put("contents",List.of(box2));

        //combine the header and body
        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body , headers);
        try {

            // we combine the url and key and request
            ResponseEntity<Map> responce =restTemplate.postForEntity(baseUrl+apiKey,request, Map.class);
            Map<String,Object> responcebody = responce.getBody();

            //important code (like find the final answer extract the amnser like Digging)
            if (responcebody !=null && responcebody.containsKey("candidates")){
                List<Map<String,Object>> Answers = (List<Map<String, Object>>) responcebody.get("candidates");
                Map<String ,Object> firstAnswer =  Answers.get(0);
                Map<String ,Object> content = (Map<String, Object>) firstAnswer.get("content");
                List<Map<String,Object>> finalans = (List<Map<String, Object>>) content.get("parts");


                String jsonText = finalans.get(0).get("text").toString();

  //remove ```json formatting if present
                jsonText = jsonText.replace("```json", "")
                        .replace("```", "")
                        .trim();
                //convert json to dto
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonText,FinalBillDto.class);


            }
        }catch (Exception e){
            throw new RuntimeException("Error " + e.getMessage()) ;
        }


        throw new RuntimeException("No responce from Api");

    }

}
