package uk.co.prodapt.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList; // Task #2

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.prodapt.inventory.model.Product;
import uk.co.prodapt.inventory.model.Supplier;

@Service
public class ProductService {

    // Original
    // private final List<Product> products = new ArrayList<>();

    // Ref: Task #2
    // Step -1: Use CopyOnWriteArrayList to Make Thread safe.
    private final List<Product> products = new CopyOnWriteArrayList<>();

    private final SupplierService supplierService;

    @Autowired
    public ProductService(SupplierService supplierService) {
        this.supplierService = supplierService;

        // Initialize some sample products
        products.add(new Product(1, "Product A", true, 1, null));
        products.add(new Product(2, "Product B", false, 2, null));
        products.add(new Product(3, "Product C", true, 3, null));
    }

//    public List<Product> getAll() {
//        return enrichWithSupplierInfo(new ArrayList<>(products));
//    }

    // Ref: Task #1
//    public List<Product> getAll(Boolean available) {
//        List<Product> filteredProducts = products;
//
//        if (available != null) {
//            filteredProducts = products.stream()
//                    .filter(product -> product.isAvailable() == available)
//                    .toList();
//        }
//
//        return enrichWithSupplierInfo(new ArrayList<>(filteredProducts));
//    }

    // Ref: Task # 2
    // Step 2. Ensure No Duplicates in Results
    // Modify your getAll(Boolean available) method to include .distinct():

    public List<Product> getAll(Boolean available) {
        List<Product> filteredProducts = products;

        if (available != null) {
            filteredProducts = products.stream()
                    .filter(product -> product.isAvailable() == available)
                    .distinct()
                    .toList();
        }

        return enrichWithSupplierInfo(new ArrayList<>(filteredProducts));
    }


    public Optional<Product> getById(Integer id) {
        return products.stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .map(this::enrichWithSupplierInfo);
    }

    public Product create(Product product) {
        products.add(product);
        return enrichWithSupplierInfo(product);
    }

    public Product update(Integer id, Product updatedProduct) {
        Product existingProduct = getById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setAvailable(updatedProduct.isAvailable());
        existingProduct.setSupplierId(updatedProduct.getSupplierId());
        return enrichWithSupplierInfo(existingProduct);
    }

    public void delete(Integer id) {
        products.removeIf(product -> product.getId().equals(id));
    }

    private List<Product> enrichWithSupplierInfo(List<Product> products) {
        for (Product product : products) {
            enrichWithSupplierInfo(product);
        }
        return products;
    }

    private Product enrichWithSupplierInfo(Product product) {
        if (product.getSupplierId() != null) {
            try {
                Supplier supplier = supplierService.getSupplierById(product.getSupplierId());
                product.setSupplierName(supplier.getName());
            } catch (RuntimeException e) {
                product.setSupplierName("Unknown Supplier");
            }
        }
        return product;
    }
}
