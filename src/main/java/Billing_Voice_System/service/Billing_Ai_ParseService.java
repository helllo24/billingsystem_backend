package Billing_Voice_System.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=";

    public FinalBillDto parerText(String input) {

        String prompt = """
                You are a billing assistant.
                Convert the given text into a structured JSON bill.
                
                STRICT RULES:
                1. Return ONLY valid JSON
                2. Extract: name, qty, price, totalprice, unit
                3. qty must be a number
                4. unit must be one of: kg, litre, piece (default = piece)
                
                PRICE RULE:
                - price = per unit price
                - totalprice = total price for that item
                
                EXAMPLES:
                - "1 kg apple 100 rs" -> price = 100, totalprice = 100
                - "5 kg onion 100 rs" -> totalprice = 100, price = 20
                
                OTHER RULES:
                5. If qty missing -> qty = 1
                6. If price missing -> price = 0
                7. If total not given -> totalprice = qty * price
                8. Support Tamil & English
                
                IMPORTANT:
                - total = sum of all totalprice
                
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

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> box1 = new HashMap<>();
        box1.put("text", prompt);

        Map<String, Object> box2 = new HashMap<>();
        box2.put("parts", List.of(box1));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(box2));
        body.put("generationConfig", Map.of("response_mime_type", "application/json"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        int maxRetries = 3;
        long backoff = 1500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(BASE_URL + apiKey, request, Map.class);
                Map<String, Object> responseBody = response.getBody();

                if (responseBody != null && responseBody.containsKey("candidates")) {
                    List<Map<String, Object>> answers = (List<Map<String, Object>>) responseBody.get("candidates");
                    Map<String, Object> firstAnswer = answers.get(0);
                    Map<String, Object> content = (Map<String, Object>) firstAnswer.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                    String jsonText = parts.get(0).get("text").toString().trim();

                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(jsonText, FinalBillDto.class);
                }
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if ((status == 503 || status == 429) && attempt < maxRetries) {
                    System.out.println("Gemini service busy (" + status + "). Retrying attempt " + (attempt + 1) + "...");
                    try {
                        Thread.sleep(backoff);
                        backoff *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                throw new RuntimeException("Gemini API Error [" + status + "]: " + e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                throw new RuntimeException("Failed parsing JSON output: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("No response received from Gemini after retry attempts");
    }
}