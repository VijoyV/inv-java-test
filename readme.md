# Yezdi Roadstar Assignment

Original Repo: 
[Bitbucket: Yezdi Roadstar Inventory](https://bitbucket.org/yezdiroadstar/inventory-ms/src/main/inventory.md)

## Task 1
- A new feature has been requested for the inventory management application. The consumer would like to add a way to filter products based on their availability status. If a product is marked as "out of stock," it should not appear in the results. If no availability filter is specified, all products should be returned.
- Ensure that this filtering is optional and configurable for each request. Update the API to support this feature and ensure it is reflected in the Swagger documentation.

### 1.A Changes Made
- Here are the few changes made to code - ProductService.Java and InventoryController.java 
- It is available with the following [commit](https://github.com/VijoyV/inv-java-test/commit/194c0661c534967248d6eb2b051fcc44b0a3a5ef):

#### 1.A.1 ProductService.java

old: 
```java
    public List<Product> getAll() {
        return enrichWithSupplierInfo(new ArrayList<>(products));
    }
```

new:

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
#### 1.A.2. Modify InventoryController.java
- Update the list() method to accept @RequestParam and related changes in body
- To improve Swagger docs, added / modified the Annotations

old:
```java
    @ApiResponse(responseCode = "200", description = "Returns list of all products", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
    @GetMapping
    public List<Product> list() {
        return productService.getAll();
    }
```

new:
```java
    @GetMapping
    @Operation(summary = "Get all products with or without filter", description = "Returns all products. Optionally filter by availability using ?available=true or false")
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

Tested teh code with Swagger UI and CURL and found to be working perfectly.

## Task 2
- An issue has been raised by a customer, and investigation has determined that the inventory management application is the source. When filtering products by availability (implemented in Task 1), the application occasionally returns incorrect results due to a race condition in the ProductService.
- To simulate this issue, assume that the ProductService may occasionally return duplicate or incomplete results. Implement a solution to ensure that the results returned by the API are always accurate and free of duplicates. Follow best practices and existing project conventions where appropriate.

### 2.A Simulating the issue to a certain extend.

#### 2.A.1 Created a JUNIT Test Class [ApiStressTestClient.java](https://github.com/VijoyV/inv-java-test/blob/4d1be5cd3e07dbeb88d9e474602329d0956ee7cf/src/main/java/uk/co/prodapt/inventory/test/ApiStressTestClient.java)

The committed code is available [here](https://github.com/VijoyV/inv-java-test/commit/baf2cd2e854abbcc64adb2b7664d16827348bc23)

#### 2.A.2 Ran and got some logs like this:

```declarative
[GET] Total=69, Distinct=68
[ERROR] pool-1-thread-21: org.springframework.web.client.HttpServerErrorException$InternalServerError: 500 : "An unexpected error occurred: null"
[POST] ID=305 → HTTP 201
[GET] Total=69, Distinct=68
[GET] Total=70, Distinct=69
[GET] Total=80, Distinct=80
[POST] ID=3480 → HTTP 201
[GET] Total=66, Distinct=65
[GET] Total=70, Distinct=69
[GET] Total=80, Distinct=80
[ERROR] pool-1-thread-23: org.springframework.web.client.HttpServerErrorException$InternalServerError: 500 : "An unexpected error occurred: null"
[POST] ID=3326 → HTTP 201

```
### 2.B Log Observation:

#### 2.B.1 **Success Pattern

Many lines like:

```
    [POST] ID=3326 → HTTP 201
``` 

confirm that your service is accepting POST requests concurrently and 
responding correctly (201 = Created).

#### 2.B.2 Data Inconsistency Under Load

The `[GET] Total=X, Distinct=Y` values fluctuate unexpectedly:
    
    ```
    [GET] Total=66, Distinct=65
    [GET] Total=70, Distinct=69
    [GET] Total=80, Distinct=80
    ```
    
This shows that **duplicates are appearing** in in-memory `productList`. 
You’re adding a `Product` with a randomly generated ID but sometimes that same ID appears more than once, suggesting **a race condition** or **lack of synchronization** in `addProduct`.

#### 2.B.3 Unhandled Exceptions / Failures

    You see multiple 500 errors:
    
    ```
      [ERROR] pool-1-thread-31: HttpServerErrorException$InternalServerError: 500 : "An unexpected error occurred: null"
    ```
    
    These are unhandled exceptions happening inside controller or service layer 

### 2.D Modifications 

The following major modifications are done to two Java code to make the code 
thread safe and avoid duplicates.

The commit link may be verified [here](https://github.com/VijoyV/inv-java-test/commit/ad1caf15ad12c7a7094ce38e2d6ef902fa47f776).

#### 2.D.1 ProductService.java

```java

    // Ref: Task #2
    // Step -1: Use CopyOnWriteArrayList to Make Thread safe.
    private final List<Product> products = new CopyOnWriteArrayList<>();
    
    ...
            
    // Ref: Task # 2
    // Step 2. Ensure No Duplicates in Results
    // Modify getAll(Boolean available) method to include .distinct():

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

#### 2.D.2. Product.java

```java

    // Code Improvement - Task #2
    // Step -3: This ensures .distinct() works correctly and prevents equality issues during deduplication.

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

### 2.E Retested 

And got the clean log like this:

```declarative
[GET] Total=189, Distinct=189
[GET] Total=201, Distinct=201
[GET] Total=201, Distinct=201
[GET] Total=201, Distinct=201
[GET] Total=189, Distinct=189
[GET] Total=189, Distinct=189
[GET] Total=189, Distinct=189
[GET] Total=201, Distinct=201
[GET] Total=201, Distinct=201

✅ Concurrency test completed.

2025-06-29 21:46:16.129  INFO 2724 --- [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2025-06-29 21:46:16.132  INFO 2724 --- [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2025-06-29 21:46:16.137  INFO 2724 --- [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.

Process finished with exit code 0

```
The concurrency test will now show Total == Distinct consistently. 
No crashes, no duplicates, and correct product data

## Task 3

The code base provided is very small and loks like for demo purpose. it does not use
a proper datastore to store data (just in-memory H2). 
Here are some recommendations tailored to the current codebase - in-memory product list, simple Spring Boot structure.

### 1. Replace In-Memory Storage with Persistent Layer (e.g., JPA)

**Why:**
* Currently, all data is stored in a `List<Product>`, which resets on every restart.
* This makes the app non-production ready and not testable across sessions.

**Suggestion:**
* Introduce a `ProductRepository` interface using Spring Data JPA.
* Migrate logic from `ProductService` to use repository methods (`findAll`, `save`, `deleteById`, etc.).
* Persist to H2/MySQL/SQLite.


### 2. Add Proper Unit & Integration Tests

**Why:**
* The code has minimal test coverage (and none that test business logic under Spring context).
* Adding tests will support regression prevention and confidence in refactors.

**Suggestion:**
* Add unit tests for `ProductService` using JUnit + Mockito.
* Add integration tests using `@SpringBootTest` for controller endpoints.

### 3. Introduce DTOs and Mappers (Avoid Exposing Model Directly)

**Why:**
* Currently, internal `Product` entities are returned directly to the client.
* This tightly couples your internal logic with the API contract.

**Suggestion:**
* Create `ProductRequestDTO` and `ProductResponseDTO` classes.
* Use [MapStruct](https://mapstruct.org/). (already a project dependency) to map between entity and DTO.


### 4. Add Global Exception Handling (Robust API Responses)

**Why:**
* Current exceptions (e.g., `RuntimeException`, `IllegalArgumentException`) may return stack traces to clients.

**Suggestion:**
* Add a `@ControllerAdvice` class to handle exceptions gracefully.
* Return standardized error JSON with fields like `timestamp`, `status`, `error`, `message`, `path`.

### 5. Improve Swagger/OpenAPI Documentation

**Why:**
* Swagger is functional, but lacks parameter descriptions and examples.

**Suggestion:**
* Use `@Parameter` on controller method arguments to explain `available=true|false`.
* Add `@Schema(example = "...")` in models.
* Add `@Operation` summary and description per endpoint.

### Summary Table

| Priority | Recommendation               | Benefit                        |
|----------| ---------------------------- | ------------------------------ |
| 1️       | Switch to JPA                | Persistent, scalable, testable |
| 2️       | Add DTOs and Mappers         | Clean API separation           |
| 3️       | Add Unit + Integration Tests | Safer development              |
| 4️       | Global Exception Handling    | API reliability & clarity      |
| 5️       | Enhance Swagger Docs         | Better developer experience    |

