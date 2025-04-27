package com.financetracker;

import java.io.IOException;

/**
 * Allows exporting financial data (e.g., to CSV, PDF, Excel).
 */
public interface Exportable {
    void exportToCSV(String filename) throws IOException;
}