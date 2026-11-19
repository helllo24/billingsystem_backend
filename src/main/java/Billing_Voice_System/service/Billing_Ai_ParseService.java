
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

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // Try the lightweight model first
    private static final String PRIMARY_MODEL = "gemini-3.5-flash-lite";

    // Fallback model
    private static final String FALLBACK_MODEL = "gemini-3.8-flash";

    private static final int MAX_ATTEMPTS = 2;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper mapper = new ObjectMapper();

    public FinalBillDto parerText(String input) {

        String prompt = """
                You are a billing assistant.
                Convert the given text into a structured JSON bill.

                STRICT RULES:
                1. Return ONLY valid JSON.
                2. Extract: name, qty, price, totalprice, unit.
                3. qty must be a number.
                4. unit must be kg, litre, or piece (default = piece).

                PRICE RULE:
                - price = per unit price.
                - totalprice = total price for that item.

                EXAMPLES:
                - "1 kg apple 100 rs" -> price = 100, totalprice = 100.
                - "5 kg onion 100 rs" -> price = 20, totalprice = 100.

                OTHER RULES:
                5. If qty missing -> qty = 1.
                6. If price missing -> price = 0.
                7. If total not given -> totalprice = qty * price.
                8. Support Tamil and English.
                9. total = sum of all totalprice.

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

        // Request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Gemini request body
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        body.put("generationConfig",
                Map.of("responseMimeType", "application/json"));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        // Primary model first, fallback model second
        List<String> models =
                List.of(PRIMARY_MODEL, FALLBACK_MODEL);

        modelLoop:
        for (String model : models) {

            long backoff = 1000;

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

                try {

                    String url = BASE_URL + model
                            + ":generateContent?key=" + apiKey;

                    System.out.println(
                            "Trying Gemini model: " + model);

                    ResponseEntity<Map> response =
                            restTemplate.postForEntity(
                                    url,
                                    request,
                                    Map.class
                            );

                    Map<?, ?> responseBody = response.getBody();

                    if (responseBody == null ||
                            !responseBody.containsKey("candidates")) {

                        throw new RuntimeException(
                                "Gemini returned an empty response");
                    }

                    List<?> candidates =
                            (List<?>) responseBody.get("candidates");

                    if (candidates == null || candidates.isEmpty()) {
                        throw new RuntimeException(
                                "Gemini returned no candidates");
                    }

                    Map<?, ?> firstCandidate =
                            (Map<?, ?>) candidates.get(0);

                    Map<?, ?> responseContent =
                            (Map<?, ?>) firstCandidate.get("content");

                    if (responseContent == null) {
                        throw new RuntimeException(
                                "Gemini response content is empty");
                    }

                    List<?> parts =
                            (List<?>) responseContent.get("parts");

                    if (parts == null || parts.isEmpty()) {
                        throw new RuntimeException(
                                "Gemini response parts are empty");
                    }

                    Map<?, ?> firstPart =
                            (Map<?, ?>) parts.get(0);

                    Object textObject = firstPart.get("text");

                    if (textObject == null) {
                        throw new RuntimeException(
                                "Gemini did not return JSON text");
                    }

                    String jsonText =
                            textObject.toString().trim();

                    System.out.println(
                            "Gemini model used: " + model);

                    System.out.println(
                            "Gemini JSON: " + jsonText);

                    // Convert JSON into FinalBillDto
                    return mapper.readValue(
                            jsonText,
                            FinalBillDto.class
                    );

                } catch (HttpStatusCodeException e) {

                    int status = e.getStatusCode().value();

                    // Retry only temporary overload / rate limit errors
                    if (status == 503 || status == 429) {

                        if (attempt < MAX_ATTEMPTS) {

                            System.out.println(
                                    model + " busy (" + status +
                                            "). Retrying attempt " +
                                            (attempt + 1));

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
                                model + " unavailable after retries.");

                        System.out.println(
                                "Switching to next Gemini model...");

                        // Move to fallback model
                        continue modelLoop;
                    }

                    // Other API errors should not be retried
                    throw new RuntimeException(
                            "Gemini API Error [" + status + "]: " +
                                    e.getResponseBodyAsString(),
                            e
                    );

                } catch (Exception e) {

                    throw new RuntimeException(
                            "Failed parsing Gemini response: " +
                                    e.getMessage(),
                            e
                    );
                }
            }
        }

        throw new RuntimeException(
                "Both Gemini models are unavailable. Please try again later."
        );
    }
}
