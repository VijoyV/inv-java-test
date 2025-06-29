package uk.co.prodapt.inventory.service;

import uk.co.prodapt.inventory.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProductServiceRaceTest {

    private static final List<Product> productList = new ArrayList<>(); // intentionally NOT thread-safe
    private static volatile boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        // Writer thread: keeps adding products
        Thread writer = new Thread(() -> {
            int i = 1;
            while (!stop) {
                productList.add(new Product(i, "Product " + i, i % 2 == 0, i % 10, null));
                System.out.println("[WRITE] Product " + i++);
                sleep(1 + new Random().nextInt(3)); // very fast writes
            }
        });

        // Reader logic (will be run in many threads)
        Runnable reader = () -> {
            while (!stop) {
                try {
                    productList.forEach(p -> {
                        // Simulate access
                        String name = p.getName();
                    });
                    System.out.println("[READ] Iterated " + productList.size());
                } catch (Exception e) {
                    System.out.println("[ERROR] " + Thread.currentThread().getName() + ": " +
                            e.getClass().getSimpleName() + " - " + e.getMessage());
                }
                sleep(2); // very frequent reads
            }
        };

        writer.start();

        // Start 10 reader threads
        List<Thread> readers = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            Thread t = new Thread(reader, "Reader-" + (j + 1));
            readers.add(t);
            t.start();
        }

        // Let it run for 5 seconds
        Thread.sleep(5000);
        stop = true;

        writer.join();
        for (Thread t : readers) {
            t.join();
        }

        System.out.println("\n✅ Aggressive simulation complete. Total products: " + productList.size());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
