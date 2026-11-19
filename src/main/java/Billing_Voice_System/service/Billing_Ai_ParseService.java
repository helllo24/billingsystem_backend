
package Billing_Voice_System.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import Billing_Voice_System.dto.FinalBillDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class Billing_Ai_ParseService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String PRIMARY_MODEL = "gemini-3.8-flash";
    private static final String FALLBACK_MODEL = "gemini-3.5-flash-lite";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public FinalBillDto parerText(String input) {

        String prompt = """
            You are a billing assistant.
            Convert the given text into a structured JSON bill.

            STRICT RULES:
            1. Return ONLY valid JSON
            2. Extract: name, qty, price, totalprice, unit
            3. qty must be a number
            4. unit must be kg, litre, or piece (default piece)

            PRICE RULE:
            - price = per unit price
            - totalprice = total price for that item

            EXAMPLES:
            - "1 kg apple 100 rs" -> price=100, totalprice=100
            - "5 kg onion 100 rs" -> price=20, totalprice=100

            OTHER RULES:
            5. If qty missing -> qty=1
            6. If price missing -> price=0
            7. If total not given -> totalprice=qty*price
            8. Support Tamil and English
            9. total = sum of all totalprice

            INPUT:
            "%s"

            OUTPUT FORMAT:
            {
              "items": [
                {
                  "name": "string",
                  "qty": 1,
                  "price": 100,
                  "totalprice": 100,
                  "unit": "kg"
                }
              ],
              "total": 100
            }
            """.formatted(input);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig",
                Map.of("response_mime_type", "application/json"));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        // First primary model, then fallback model
        List<String> models = List.of(PRIMARY_MODEL, FALLBACK_MODEL);

        for (String model : models) {

            int maxRetries = 3;
            long backoff = 1500;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {

                try {
                    String url = BASE_URL + model
                            + ":generateContent?key=" + apiKey;

                    ResponseEntity<Map> response =
                            restTemplate.postForEntity(
                                    url, request, Map.class);

                    Map<?, ?> responseBody = response.getBody();

                    if (responseBody == null ||
                            !responseBody.containsKey("candidates")) {
                        throw new RuntimeException(
                                "Gemini returned an empty response");
                    }

                    List<?> candidates =
                            (List<?>) responseBody.get("candidates");

                    Map<?, ?> firstCandidate =
                            (Map<?, ?>) candidates.get(0);

                    Map<?, ?> responseContent =
                            (Map<?, ?>) firstCandidate.get("content");

                    List<?> parts =
                            (List<?>) responseContent.get("parts");

                    Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);

                    String jsonText =
                            firstPart.get("text").toString().trim();

                    System.out.println("Gemini model used: " + model);
                    System.out.println("Gemini JSON: " + jsonText);

                    return mapper.readValue(
                            jsonText, FinalBillDto.class);

                } catch (HttpStatusCodeException e) {

                    int status = e.getStatusCode().value();

                    if (status == 503 || status == 429) {

                        if (attempt < maxRetries) {

                            System.out.println(
                                    model + " busy (" + status +
                                            "). Retry " + (attempt + 1));

                            try {
                                Thread.sleep(backoff);
                                backoff *= 2;
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(
                                        "Gemini retry interrupted", ie);
                            }

                            continue;
                        }

                        System.out.println(
                                model + " unavailable. Trying next model.");

                        break;
                    }

                    throw new RuntimeException(
                            "Gemini API Error [" + status + "]: " +
                                    e.getResponseBodyAsString(), e);
                }
                catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to parse Gemini response: " +
                                    e.getMessage(), e);
                }
            }
        }

        throw new RuntimeException(
                "Both Gemini models are currently unavailable. Please try again later.");
    }
}
