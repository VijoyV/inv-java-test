package uk.co.prodapt.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.co.prodapt.inventory.exception.ProductNotFoundException;
import uk.co.prodapt.inventory.model.Product;
import uk.co.prodapt.inventory.service.ProductService;

@RestController
@RequestMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class InventoryController {

    private final ProductService productService;

    @Autowired
    public InventoryController(ProductService productService) {
        this.productService = productService;
    }

//    @ApiResponse(responseCode = "200", description = "Returns list of all products", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
//    @GetMapping
//    public List<Product> list() {
//        return productService.getAll();
//    }

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


    @ApiResponse(responseCode = "200", description = "Product returned", content = @Content(schema = @Schema(implementation = Product.class)))
    @GetMapping("/{id}")
    public Product get(@PathVariable Integer id) {
        return productService.getById(id).orElseThrow(() -> new ProductNotFoundException(String.valueOf(id)));
    }

    @ApiResponse(responseCode = "201", description = "Product successfully created", content = @Content(schema = @Schema(implementation = Product.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Product post(@Valid @RequestBody Product product) {
        return productService.create(product);
    }

    @ApiResponse(responseCode = "200", description = "Product successfully updated", content = @Content(schema = @Schema(implementation = Product.class)))
    @PutMapping("/{id}")
    public Product put(@PathVariable Integer id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    @ApiResponse(responseCode = "204", description = "Product successfully deleted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        productService.delete(id);
    }
}