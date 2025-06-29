# Yezdi Roadstar Assignment

- Original Repo: 
[Bitbucket: Yezdi Roadstar Inventory](https://bitbucket.org/yezdiroadstar/inventory-ms/src/main/inventory.md)

## Task 1

- A new feature has been requested for the inventory management application. The consumer would like to add a way to filter products based on their availability status. If a product is marked as "out of stock," it should not appear in the results. If no availability filter is specified, all products should be returned.
- Ensure that this filtering is optional and configurable for each request. Update the API to support this feature and ensure it is reflected in the Swagger documentation.

### Changes Made

- Here are the few changes made to code - ProductService.Java and InventoryController.java 
- It is available with the following commit:

#### 1. Modify ProductService.java

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
#### 2. Modify InventoryController.java
- Update the list() method to accept @RequestParam and related changes in body
- To improve Swagger docs, modify the param like this

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

## Task 2
- An issue has been raised by a customer, and investigation has determined that the inventory management application is the source. When filtering products by availability (implemented in Task 1), the application occasionally returns incorrect results due to a race condition in the ProductService.
- To simulate this issue, assume that the ProductService may occasionally return duplicate or incomplete results. Implement a solution to ensure that the results returned by the API are always accurate and free of duplicates. Follow best practices and existing project conventions where appropriate.

### Simulating the issue to a certain extend.

#### 1. Created a JUNIT Test Class [ApiStressTestClient.java](https://github.com/VijoyV/inv-java-test/blob/4d1be5cd3e07dbeb88d9e474602329d0956ee7cf/src/main/java/uk/co/prodapt/inventory/test/ApiStressTestClient.java)

#### 2. Ran and got some logs like this:

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
### Observation:

#### 1. **Success Pattern:**

Many lines like:

```
[POST] ID=3326 → HTTP 201
```

confirm that your service is accepting POST requests concurrently and responding correctly (201 = Created).

#### 2. **Data Inconsistency Under Load:**

* The `[GET] Total=X, Distinct=Y` values fluctuate unexpectedly:

```
[GET] Total=66, Distinct=65
[GET] Total=70, Distinct=69
[GET] Total=80, Distinct=80
```

This shows that **duplicates are appearing** in in-memory `productList`. 
You’re adding a `Product` with a randomly generated ID but sometimes that same ID appears more than once, suggesting **a race condition** or **lack of synchronization** in `addProduct`.

#### 3. **Unhandled Exceptions / Failures:**

You see multiple 500 errors:

```
  [ERROR] pool-1-thread-31: HttpServerErrorException$InternalServerError: 500 : "An unexpected error occurred: null"
```

* These are unhandled exceptions happening inside controller or service layer 

### **Conclusion: What the Logs Confirm**

| Symptom                     | What it Tells Us                                          | Root Cause                        |
| --------------------------- | --------------------------------------------------------- | --------------------------------- |
| **GET total > distinct**    | Duplicate IDs or same object added multiple times         | **Lack of synchronization**       |
| **GET total < expected**    | Not all successful POSTs are reflected in data            | **Lost updates**, race conditions |
| **500 Errors**              | Unhandled exception inside service/controller             | **Concurrent access issue**       |
| **Inconsistent GET counts** | Different threads seeing different views of `productList` | **Non-thread-safe list**          |
 
### Modifications 

The following Modification done to

#### 1. ProductService.java


#### 2. Product.java


