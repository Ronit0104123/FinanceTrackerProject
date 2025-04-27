package com.financetracker;

import java.io.IOException;

public interface Exportable {
    void exportToCSV(String filename) throws IOException;
}