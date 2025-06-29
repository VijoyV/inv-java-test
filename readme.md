# Yezdi Roadstar Assignment

**Original Repo:**
[Bitbucket: Yezdi Roadstar Inventory](https://bitbucket.org/yezdiroadstar/inventory-ms/src/main/inventory.md)

---

## Task 1: Add Availability-Based Filtering to Product List

* A new feature has been requested for the inventory management application to filter products by **availability status**.
* Products marked **"out of stock"** should be excluded if the filter is applied.
* If no filter is specified, **all products** should be returned.
* The filter must be **optional** and **configurable per request**.
* The Swagger documentation must be updated to reflect this feature.

---

### 1.A: Code Changes

#### Files Modified

* `ProductService.java`
* `InventoryController.java`
  Commit: [View Commit](https://github.com/VijoyV/inv-java-test/commit/194c0661c534967248d6eb2b051fcc44b0a3a5ef)

---

#### 1.A.1 `ProductService.java`

**Before:**

```java
public List<Product> getAll() {
    return enrichWithSupplierInfo(new ArrayList<>(products));
}
```

**After:**

```java
// Ref: Task #1
public List<Product> getAll(Boolean available) {
    List<Product> filteredProducts = products;

    if (available != null) {
        filteredProducts = products.stream()
                .filter(product -> product.isAvailable() == available)
                .toList();
    }

    return enrichWithSupplierInfo(new ArrayList<>(filteredProducts));
}
```

---

#### 1.A.2 `InventoryController.java`

* Added `@RequestParam` to `list()` method to support availability filtering.
* Enhanced Swagger documentation with `@Operation` and `@Parameter`.

**Before:**

```java
@ApiResponse(responseCode = "200", description = "Returns list of all products", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
@GetMapping
public List<Product> list() {
    return productService.getAll();
}
```

**After:**

```java
@GetMapping
@Operation(
    summary = "Get all products with or without filter",
    description = "Returns all products. Optionally filter by availability using ?available=true or false"
)
@ApiResponse(
    responseCode = "200",
    description = "Returns list of all products",
    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class)))
)
public List<Product> list(
        @Parameter(description = "Filter products by availability (true/false)")
        @RequestParam(required = false) Boolean available) {
    return productService.getAll(available);
}
```

Tested using Swagger UI and CURL — works as expected.

---

## Task 2: Resolve Concurrency & Race Conditions

An issue was reported where filtering products by availability occasionally returns **inconsistent or duplicate** results — likely due to **race conditions** in the in-memory `ProductService`.

---

### 2.A: Simulating the Problem

#### 2.A.1 JUnit Stress Test

Class: [`ApiStressTestClient.java`](https://github.com/VijoyV/inv-java-test/blob/4d1be5cd3e07dbeb88d9e474602329d0956ee7cf/src/main/java/uk/co/prodapt/inventory/test/ApiStressTestClient.java)
Commit: [baf2cd2e](https://github.com/VijoyV/inv-java-test/commit/baf2cd2e854abbcc64adb2b7664d16827348bc23)

#### 2.A.2 Sample Output Logs:

```text
[GET] Total=69, Distinct=68
[ERROR] pool-1-thread-21: 500 : "An unexpected error occurred: null"
[POST] ID=305 → HTTP 201
...
[GET] Total=70, Distinct=69
```

---

### 2.B: Observations

#### 2.B.1 Success Patterns

```text
[POST] ID=3326 → HTTP 201
```

→ POST requests are accepted concurrently without issue.

#### 2.B.2 Duplicate Entries

```text
[GET] Total=70, Distinct=69
```

→ Indicates duplicates in `productList`, due to race condition or lack of thread-safety.

#### 2.B.3 Unhandled Exceptions

```text
[ERROR] pool-1-thread-31: 500 : "An unexpected error occurred: null"
```

→ Null errors indicate lack of error handling in controller or service.

---

### 2.D: Code Fixes & Improvements

Commit: [ad1caf15](https://github.com/VijoyV/inv-java-test/commit/ad1caf15ad12c7a7094ce38e2d6ef902fa47f776)

---

#### 2.D.1 `ProductService.java`

```java
// Step 1: Use thread-safe data structure
private final List<Product> products = new CopyOnWriteArrayList<>();

// Step 2: Ensure duplicates are removed
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
```

---

#### 2.D.2 `Product.java`

```java
// Step 3: Override equals() and hashCode() to support deduplication
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Product product = (Product) o;
    return id != null && id.equals(product.id);
}

@Override
public int hashCode() {
    return id != null ? id.hashCode() : 0;
}
```

---

### 2.E Retesting Results

```text
[GET] Total=201, Distinct=201
[GET] Total=189, Distinct=189
...
Concurrency test completed.

INFO ...: Shutdown completed.
```

→ Total = Distinct
→ No 500 errors
→ Consistent product data

---

## Task 3: Code Review & Recommendations

The codebase is small and uses in-memory H2 storage — suitable for demos but not production. Below are improvements based on best practices.


### 1. Replace In-Memory List with JPA

**Why:** Data resets on restart

**How:**
* Create `ProductRepository` using Spring Data JPA
* Persist to H2/MySQL
* Replace all `.add`, `.remove`, `.getAll` with repository methods


### 2. Add Unit & Integration Tests

**Why:**  No regression safety

**How:**

* Use JUnit + Mockito for `ProductService`
* Use `@SpringBootTest` for controller testing

---

### 3. Use DTOs & MapStruct

**Why:** Directly exposing entity leaks internals

**How:**

* Create `ProductRequestDTO` & `ProductResponseDTO`
* Use MapStruct to convert between entity and DTOs

### 4. Global Exception Handling

**Why:** Raw exceptions are sent to clients

**How:**

* Create a `@ControllerAdvice` class
* Return error JSON with:
  `timestamp`, `status`, `error`, `message`, `path`

### 5. Improve Swagger/OpenAPI Docs

**Why:** Lacks parameter details

**How:**

* Use `@Parameter` with examples
* Add `@Schema(example = "...")` in models
* Add meaningful `@Operation` summaries


## Summary Table

| Priority | Recommendation               | Benefit                        |
| -------- | ---------------------------- | ------------------------------ |
| High     | Switch to JPA                | Persistent, scalable, testable |
| Medium   | Add DTOs and Mappers         | Clean API separation           |
| Medium   | Add Unit + Integration Tests | Safer development              |
| Low      | Global Exception Handling    | API reliability & clarity      |
| Low      | Enhance Swagger Docs         | Better developer experience    |

