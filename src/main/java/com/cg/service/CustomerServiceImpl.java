package com.cg.service;

import com.cg.exception.InvalidRequestException;
import com.cg.model.Customer;
import com.cg.repository.CustomerRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public void importToDb(MultipartFile multipartfile) {

        if (!multipartfile.isEmpty()) {
            List<Customer> customers = new ArrayList<>();

            try {
                XSSFWorkbook workBook = new XSSFWorkbook(multipartfile.getInputStream());

                XSSFSheet sheet = workBook.getSheetAt(0);
                // looping through each row
                for (int rowIndex = 0; rowIndex < getNumberOfNonEmptyCells(sheet, 0); rowIndex++) {
                    // current row
                    XSSFRow row = sheet.getRow(rowIndex);
                    // skip the first row because it is a header row
                    if (rowIndex == 0) {
                        continue;
                    }

                    String fullName = String.valueOf(getValue(row.getCell(0)));
                    String email = String.valueOf(getValue(row.getCell(1)));
                    String phone = String.valueOf(getValue(row.getCell(2)));
                    String address = String.valueOf(getValue(row.getCell(3)));
                    BigDecimal balance = BigDecimal.valueOf(Long.parseLong(String.valueOf(getValue(row.getCell(4)))));

                    Customer transaction = Customer.builder()
                            .fullName(fullName)
                            .email(email)
                            .phone(phone)
                            .address(address)
                            .balance(balance)
                            .build();

                    customers.add(transaction);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!customers.isEmpty()) {
                // save to database
                customerRepository.saveAll(customers);
            }
        }
    }

    @Override
    public StreamingResponseBody exportToExcel(HttpServletResponse response) {

        List<Customer> customers = customerRepository.findAll();

        if (customers.isEmpty()) {
            throw new InvalidRequestException("No data found in database");
        }

        return outputStream -> {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSFWorkbook.DEFAULT_WINDOW_SIZE)) {
                // Creating excel sheet
                String sheetName = "Customers";
                Sheet sheet = workbook.createSheet(sheetName);

                // Creating font style for excel sheet
                Font headerFont = workbook.createFont();
                headerFont.setColor(IndexedColors.BLACK.getIndex());

                CellStyle headerColumnStyle = workbook.createCellStyle();
                headerColumnStyle.setFont(headerFont);

                // Row for header at 0 index
                Row headerRow = sheet.createRow(0);

                // Name of the columns to be added in the sheet
                String[] columns = new String[] { "id", "FullName", "Email", "Phone", "Address", "Balance" };

                // Creating header column at the first row
                for (int i = 0; i < columns.length; i++) {
                    Cell headerColumn = headerRow.createCell(i);
                    headerColumn.setCellValue(columns[i]);
                    headerColumn.setCellStyle(headerColumnStyle);
                }

                // Date formatting
                CellStyle dataColumnDateFormatStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                dataColumnDateFormatStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yy h:mm;@"));

                // Adding data to sheet from the second row
                int rowIndex = 1;
                for (Customer customer : customers) {
                    // Creating row for writing data
                    Row dataRow = sheet.createRow(rowIndex);

                    Cell columnId = dataRow.createCell(0);
                    columnId.setCellValue(customer.getId() != null ? customer.getId() : -1);

                    Cell columnFullName = dataRow.createCell(1);
                    columnFullName.setCellValue(customer.getFullName() != null ? customer.getFullName() : "");

                    Cell columnEmail = dataRow.createCell(2);
                    columnEmail.setCellValue(customer.getEmail() != null ? customer.getEmail() : "");

                    Cell columnPhone = dataRow.createCell(3);
                    columnPhone.setCellValue(customer.getPhone() != null ? customer.getPhone() : "");

                    Cell columnAddress = dataRow.createCell(4);
                    columnAddress.setCellValue(customer.getAddress() != null ? customer.getAddress() : "");

                    Cell columnBalance = dataRow.createCell(5);
                    columnBalance.setCellValue((customer.getBalance().toString() != null ? customer.getBalance().toString() : ""));

                    // Incrementing rowIndex by 1
                    rowIndex++;
                }

                workbook.write(out);
                workbook.dispose();

                String filename = "Export-customers-data" + ".xlsx";
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                response.setContentLength(out.size());

                InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
                int BUFFER_SIZE = 1024;
                int bytesRead;
                byte[] buffer = new byte[BUFFER_SIZE];

                // Writing to output stream
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        };
    }

    private Object getValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case ERROR:
                return cell.getErrorCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            case _NONE:
                return null;
            default:
                break;
        }
        return null;
    }

    public static int getNumberOfNonEmptyCells(XSSFSheet sheet, int columnIndex) {
        int numOfNonEmptyCells = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                XSSFCell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    numOfNonEmptyCells++;
                }
            }
        }
        return numOfNonEmptyCells;
    }
}
