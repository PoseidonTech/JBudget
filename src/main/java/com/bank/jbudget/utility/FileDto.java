package com.bank.jbudget.utility;

import com.bank.jbudget.domain.CsvTransaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FileDto {

    private static final Logger log = Logger.getLogger(FileDto.class);

    private final CsvTransactionTagManager csvTransactionTagManager;

    public FileDto(CsvTransactionTagManager csvTransactionTagManager) {
        this.csvTransactionTagManager = csvTransactionTagManager;
    }

    public List<CsvTransaction> csvFileImport() {
        List<CsvTransaction> transactionList = new ArrayList<>();
        List<CSVRecord> csvRecordList = new ArrayList<>();//initialize as empty
        String accountHistoryPath = ClassLoader.getSystemResource("accountHistory.csv").toString();

        try (BufferedReader br = new BufferedReader(new FileReader(accountHistoryPath))) {
            CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(br);
            csvRecordList = csvParser.getRecords();
        } catch (IOException ioEx) {
            log.error(ioEx);
        }

        csvTransactionTagManager.seedBaseTags();

        String xsdPath = ClassLoader.getSystemResource("transaction.xsd").toString();

        for (CSVRecord csvRecord : csvRecordList) {
            try {
                //validate transaction against xsd
                Document doc = XmlUtils.getTransactionXmlDoc(csvRecord);

                XmlUtils.validate(new DOMSource(doc), xsdPath);

                //if valid (no exceptions thrown) create CsvTransaction and add to list
                CsvTransaction csvTransaction = new CsvTransaction(csvRecord);
                csvTransaction.setTagList(csvTransactionTagManager.addDefaultTags(csvTransaction.getDescription()));
                transactionList.add(csvTransaction);

            } catch (final IOException | ParseException | ParserConfigurationException | TransformerException | SAXException ex) {
                log.error(ex);
            }
        }

        return transactionList;
    }
}