package Billing_Voice_System.service;


import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class Billing_SpeechService {

    @Value("${assemAI}")
    private String apiKey;

    private final String UPLOAD_URL = "https://api.assemblyai.com/v2/upload";
    private final String TRANSCRIPT_URL = "https://api.assemblyai.com/v2/transcript";

    public String convertAudioToText(byte[] audioData) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);

        try {
            // 1. Upload the audio file
            HttpEntity<byte[]> uploadRequest = new HttpEntity<>(audioData, headers);
            ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(UPLOAD_URL, uploadRequest, Map.class);
            String audioUrl = (String) uploadResponse.getBody().get("upload_url");

            // 2. Start the transcription
            // FIX: Use "speech_models" with a List.of("best")
            Map<String, Object> transcriptJson = Map.of(
//                    "audio_url", audioUrl,
//                    "speech_models", List.of("universal-3-pro", "universal-2"), // List of both models
//                    "language_code", "en"
                    "audio_url", audioUrl,
                    "speech_models", List.of("universal-3-pro"), // ✅ REQUIRED
                    "language_code", "en",                       // ✅ force English
                    "punctuate", true,
                    "format_text", true
            );

            HttpEntity<Map<String, Object>> transcriptRequest = new HttpEntity<>(transcriptJson, headers);
            ResponseEntity<Map> transcriptResponse = restTemplate.postForEntity(TRANSCRIPT_URL, transcriptRequest, Map.class);
            String transcriptId = (String) transcriptResponse.getBody().get("id");

            // 3. Polling for results
            while (true) {
                ResponseEntity<Map> resultResponse = restTemplate.exchange(
                        TRANSCRIPT_URL + "/" + transcriptId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

                String status = (String) resultResponse.getBody().get("status");
                if ("completed".equals(status)) {
                    String rawText = (String) resultResponse.getBody().get("text");

                    // 🔥 apply normalization
                    String cleanText = normalize(rawText);

                    // debug (very important)
                    System.out.println("RAW TEXT: " + rawText);
                    System.out.println("CLEAN TEXT: " + cleanText);

                    return cleanText;
                }
                Thread.sleep(1500);
            }
        } catch (Exception e) {
            throw new RuntimeException("Speech Error: " + e.getMessage());
        }
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replace("one", "1")
                .replace("two", "2")
                .replace("three", "3")
                .replace("four", "4")
                .replace("five", "5")

                // 🔥 fix speech mistakes
                .replace("kilo", "kg")
                .replace("k g", "kg")
                .replace("rupees", "rs")

                // 🔥 try to fix misheard words
                .replace("oriculo", "kilo")   // temp fix for your case
                .replace("happy", "apple")    // temp fix

                .trim();
    }
}




