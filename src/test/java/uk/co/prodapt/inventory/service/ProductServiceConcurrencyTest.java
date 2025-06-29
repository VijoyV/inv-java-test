/**
 *  Introduced this Unit Test code to simulate race condition with old Product & ProductService code
 *  And to re-test again with new Product & ProductService code
 *
 */
package uk.co.prodapt.inventory.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ProductServiceConcurrencyTest {

    private static final String BASE_URL = "http://localhost:8080/products";

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void stressTestProductService() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        int postThreads = 20;
        int getThreads = 30;

        CountDownLatch latch = new CountDownLatch(postThreads + getThreads);

        for (int i = 0; i < postThreads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        int id = new Random().nextInt(5000);
                        boolean available = id % 2 == 0;
                        int supplierId = id % 10;

                        JSONObject json = new JSONObject();
                        json.put("id", id);
                        json.put("name", "P-" + id);
                        json.put("available", available);
                        json.put("supplierId", supplierId);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<String> request = new HttpEntity<>(json.toString(), headers);

                        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL, request, String.class);
                        System.out.println("[POST] ID=" + id + " → HTTP " + response.getStatusCodeValue());
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] " + Thread.currentThread().getName() + ": " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < getThreads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        boolean filter = new Random().nextBoolean();
                        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "?available=" + filter, String.class);
                        JSONArray array = new JSONArray(response.getBody());
                        Set<String> distinctNames = new HashSet<>();

                        for (int k = 0; k < array.length(); k++) {
                            distinctNames.add(array.getJSONObject(k).getString("name"));
                        }

                        System.out.printf("[GET] Total=%d, Distinct=%d%n", array.length(), distinctNames.size());
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] " + Thread.currentThread().getName() + ": " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();
        System.out.println("\n✅ Concurrency test completed.");
    }
}
