package uk.co.prodapt.inventory.test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class ApiStressTestClient {

    private static final String BASE_URL = "http://localhost:8080/products";
    private static volatile boolean stop = false;

    public static void main(String[] args) throws Exception {
        // POST thread
        Runnable poster = () -> {
            int id = 1000;
            while (!stop) {
                try {
                    boolean available = id % 2 == 0;
                    int supplierId = id % 10;
                    String payload = String.format(
                            "{\"id\": %d, \"name\": \"P-%d\", \"available\": %b, \"supplierId\": %d}",
                            id, id, available, supplierId
                    );
                    HttpURLConnection con = (HttpURLConnection) new URL(BASE_URL).openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setDoOutput(true);
                    OutputStream os = con.getOutputStream();
                    os.write(payload.getBytes());
                    os.flush();
                    os.close();
                    int code = con.getResponseCode();
                    System.out.println("[POST] ID=" + id + " → HTTP " + code);
                    con.disconnect();
                } catch (Exception e) {
                    System.out.println("[POST ERROR] " + e);
                }
                id++;
                sleep(50 + new Random().nextInt(100)); // slower than reads
            }
        };

        // GET thread
        Runnable getter = () -> {
            while (!stop) {
                try {
                    boolean filter = new Random().nextBoolean();
                    URL url = new URL(BASE_URL + "?available=" + filter);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    int code = con.getResponseCode();
                    System.out.println("[GET] available=" + filter + " → HTTP " + code);
                    con.disconnect();
                } catch (Exception e) {
                    System.out.println("[GET ERROR] " + e);
                }
                sleep(10 + new Random().nextInt(30));
            }
        };

        // Launch threads
        Thread postThread = new Thread(poster);
        Thread[] readers = new Thread[5];
        for (int i = 0; i < 5; i++) {
            readers[i] = new Thread(getter, "Getter-" + (i + 1));
            readers[i].start();
        }
        postThread.start();

        // Let it run for 15 seconds
        Thread.sleep(15000);
        stop = true;

        postThread.join();
        for (Thread t : readers) {
            t.join();
        }

        System.out.println("\n✅ Stress test completed.");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
