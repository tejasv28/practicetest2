package com.example.webhook_solver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("started runner");
        // TODO: maybe move this url to application.properties later
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        
        RestTemplate rest = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> req = new HashMap<>();
        req.put("name", "John Doe");
        req.put("regNo", "REG12347");
        req.put("email", "john@example.com");
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);
        
        System.out.println("calling generateWebhook");
        ResponseEntity<Map> res = rest.postForEntity(url, entity, Map.class);
        
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            Map body = res.getBody();
            String webhook = (String) body.get("webhook");
            String token = (String) body.get("accessToken");
            
            // System.out.println("got webhook " + webhook);
            
            if (token == null) {
                System.out.println("err: no token");
                return;
            }
            
            // q1 2nd highest
            String sql = "SELECT p1.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                         "FLOOR(DATEDIFF(CURRENT_DATE, e.DOB) / 365.25) AS AGE, d.DEPARTMENT_NAME " +
                         "FROM PAYMENTS p1 " +
                         "JOIN EMPLOYEE e ON p1.EMP_ID = e.EMP_ID " +
                         "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                         "WHERE 1 = (SELECT COUNT(DISTINCT p2.AMOUNT) FROM PAYMENTS p2 WHERE p2.AMOUNT > p1.AMOUNT);";
                         
            System.out.println("submitting answer");
            
            HttpHeaders subHeaders = new HttpHeaders();
            subHeaders.setContentType(MediaType.APPLICATION_JSON);
            // no Bearer prefix
            subHeaders.set("Authorization", token);
            
            Map<String, String> subReq = new HashMap<>();
            subReq.put("finalQuery", sql);
            HttpEntity<Map<String, String>> subEntity = new HttpEntity<>(subReq, subHeaders);
            
            int retries = 0;
            boolean success = false;
            
            while (retries < 3 && !success) {
                try {
                    ResponseEntity<String> subRes = rest.postForEntity(webhook, subEntity, String.class);
                    System.out.println("done! code: " + subRes.getStatusCode());
                    success = true;
                } catch (Exception e) {
                    retries++;
                    System.out.println("fail attempt " + retries + ": " + e.getMessage());
                    if (retries < 3) {
                        try {
                            Thread.sleep(2000); // just wait 2 sec
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }
}
