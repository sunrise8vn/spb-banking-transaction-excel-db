package com.cg.controller.rest;

import com.cg.model.Customer;
import com.cg.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping(path = "/api/customers")
public class CustomerRestController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<?> listCustomers() {
        List<Customer> customers = customerService.findAll();
        return ResponseEntity.ok(customers);
    }

    @PostMapping(path = "/import-to-db")
    public ResponseEntity<?> importTransactionsFromExcelToDb(@RequestPart MultipartFile file) {

        customerService.importToDb(file);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/export-to-excel")
    public ResponseEntity<StreamingResponseBody> downloadTransactions(HttpServletResponse response) {

        StreamingResponseBody responseBody = customerService.exportToExcel(response);

        return ResponseEntity.ok(responseBody);

    }
}