# Finance Tracker

A Java-based personal finance management application that helps users track their income, expenses, budgets, and financial goals.

## Features

- Track income and expenses
- Set and monitor budgets
- Create and track financial goals
- Schedule recurring payments
- Generate expense charts and reports
- Export data to CSV
- Secure authentication (OTP/Biometric)

## Requirements

- Java 8 or higher
- JFreeChart library (included in lib/)

## Quick Start

1. Clone the repository
2. Compile: `javac -cp "lib/*" src/com/financetracker/*.java`
3. Run: `java -cp "lib/*:src" com.financetracker.FinanceTracker`

## Usage

The application provides an interactive menu for:
- Adding income/expenses
- Setting budgets
- Creating goals
- Scheduling payments
- Viewing summaries
- Exporting data
- Generating visualizations

## Security

- OTP authentication (default code: 123456)
- Data encryption for backups
- Cloud backup support
