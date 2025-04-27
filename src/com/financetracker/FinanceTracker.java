// File: FinanceTracker.java
package com.financetracker;

// Core imports
import java.util.*;     // List, ArrayList, Date, Timer, TimerTask, Map, HashMap, Scanner
import java.io.*;       // PrintWriter, IOException

// JFreeChart imports for visualization
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/**
 * Main application for personal finance tracking.
 * Supports:
 *  - secure login (OTP/biometric)
 *  - income/expense recording
 *  - category budgets with alerts
 *  - savings/debt goals
 *  - automated recurring payments
 *  - simple summaries & CSV export
 *  - data visualization (charts)
 *  - stubs for encryption & cloud backup
 */
public class FinanceTracker implements Exportable, Notification {

    // ----- Core state -----
    private double balance = 0;                               // current available balance
    private List<Transaction> transactions = new ArrayList<>(); // all income & expenses
    private List<Budget> budgets = new ArrayList<>();  // user-defined budgets
    private List<Goal> goals = new ArrayList<>();  // user-defined savings/debt goals
    private Timer timer = new Timer();        // schedules recurring payments

    // =====================================================================
    //                         TRANSACTION CLASSES
    // =====================================================================

    /**
     * Abstract base class for any financial transaction.
     * Subclasses must implement process() to update balance and enforce rules.
     */
    abstract class Transaction {
        double amount;   // positive value for income or expense
        String category; // user-defined category label (e.g., "Groceries")
        Date date;       // timestamp of when this transaction was created

        /** 
         * Constructor for any transaction.
         * @param amount   the transaction amount
         * @param category the transaction category
         */
        Transaction(double amount, String category) {
            this.amount = amount;
            this.category = category;
            this.date = new Date();
        }

        /** 
         * Apply this transaction to the balance, possibly throwing exceptions
         * (e.g., for insufficient funds or budget exceed).
         */
        abstract void process() throws Exception;
    }

    /** Records incoming money. */
    class Income extends Transaction {
        Income(double amt, String cat) { super(amt, cat); }
        @Override
        void process() {
            balance += amount;  // add to balance
        }
    }

    /** Records outgoing money and enforces budgets. */
    class Expense extends Transaction {
        Expense(double amt, String cat) { super(amt, cat); }

        @Override
        void process() throws Exception {
            // 1) Check sufficient funds
            if (amount > balance) {
                throw new Exception("Insufficient funds");
            }
            // 2) Deduct from balance
            balance -= amount;
            // 3) Enforce any matching budget
            for (Budget b : budgets) {
                if (b.category.equals(category) && amount > b.limit) {
                    throw new Exception("Budget exceeded for " + category);
                }
            }
        }
    }

    // =====================================================================
    //                                 BUDGET
    // =====================================================================

    /**
     * Tracks a spending limit for a given category and period.
     */
    class Budget {
        String category; // category this budget applies to
        double limit;    // max allowed spend in the period
        Period period;   // timeframe: fortnightly, monthly, or trimester

        /** Standard constructor. */
        Budget(String category, double limit, Period period) {
            this.category = category;
            this.limit = limit;
            this.period = period;
        }
    }

    /** Supported budget periods. */
    enum Period { FORTNIGHTLY, MONTHLY, TRIMESTER }

    // =====================================================================
    //                                   GOAL
    // =====================================================================

    /**
     * A savings (or debt payoff) goal.
     * Tracks target amount and current progress.
     */
    class Goal {
        String name;    // descriptive name of the goal
        double target;  // target amount to reach
        double progress;// current amount saved/paid

        /** Constructor for a new goal. */
        Goal(String name, double target) {
            this.name = name;
            this.target = target;
            this.progress = 0;
        }

        /** Add progress (positive for income, negative for expense). */
        void update(double amt) {
            progress += amt;
        }

        /** Check if the goal is achieved. */
        boolean isAchieved() {
            return progress >= target;
        }

        /** Human-readable status. */
        String status() {
            return name + ": " + progress + "/" + target;
        }
    }

    // =====================================================================
    //                            RECURRING PAYMENTS
    // =====================================================================

    /**
     * Models an automated recurring expense (e.g., monthly bill or EMI).
     * Scheduled via a Java Timer.
     */
    class RecurringPayment extends TimerTask {
        double amount;
        String category;

        RecurringPayment(double amt, String cat) {
            this.amount   = amt;
            this.category = cat;
        }

        /** Called by Timer at each period. */
        public void run() {
            try {
                addExpense(amount, category);
            } catch (Exception e) {
                sendAlert(e.getMessage());
            }
        }
    }

    // =====================================================================
    //                              UTILITIES (STUBS)
    // =====================================================================

    /** Stub for encrypting data at rest (e.g., AES). */
    static class CryptoUtil {
        static void encryptAndSave(Object data, String filename) {
            // TODO: integrate real AES encryption here
        }
    }

    /** Stub for uploading backups to the cloud. */
    static class CloudBackup {
        static void upload(String filename) {
            // TODO: wire up with AWS/GCP/Azure SDK
            System.out.println("Backing up " + filename);
        }
    }

    /** Handles drawing & saving charts via JFreeChart. */
    static class DataVisualizer {
        /**
         * Generate two PNGs in the working dir:
         *  - expense_trend.png : line chart over time
         *  - category_pie.png  : pie chart by category
         */
        static void generateCharts(List<Transaction> txns) {
            try {
                // Build dataset for line chart
                DefaultCategoryDataset lineDs = new DefaultCategoryDataset();
                for (Transaction t : txns) {
                    if (t instanceof Expense) {
                        String day = t.date.toString().substring(0,10);
                        lineDs.addValue(t.amount, t.category, day);
                    }
                }
                var lineChart = ChartFactory.createLineChart(
                    "Expense Trend", "Date", "Amount", lineDs
                );
                ChartUtilities.saveChartAsPNG(
                    new File("expense_trend.png"), lineChart, 800, 600
                );

                // Build dataset for pie chart
                DefaultPieDataset pieDs = new DefaultPieDataset();
                Map<String, Double> sums = new HashMap<>();
                for (Transaction t : txns) {
                    if (t instanceof Expense) {
                        sums.merge(t.category, t.amount, Double::sum);
                    }
                }
                sums.forEach(pieDs::setValue);

                var pieChart = ChartFactory.createPieChart(
                    "Expenses by Category", pieDs, true, true, false
                );
                ChartUtilities.saveChartAsPNG(
                    new File("category_pie.png"), pieChart, 800, 600
                );

                System.out.println("Charts saved: expense_trend.png, category_pie.png");
            } catch (Exception e) {
                System.out.println("Chart error: " + e.getMessage());
            }
        }
    }

    // =====================================================================
    //                         AUTHENTICATION INTERFACES
    // =====================================================================

    /** Generic auth contract (OTP, biometric, etc.). */
    interface AuthenticationMethod {
        boolean authenticate(String credential);
    }

    /** Stub OTP authentication (expects "123456"). */
    class OTPAuthentication implements AuthenticationMethod {
        public boolean authenticate(String otp) {
            return "123456".equals(otp);
        }
    }

    /** Stub biometric authentication (always true). */
    class BiometricAuthentication implements AuthenticationMethod {
        public boolean authenticate(String data) {
            return true;
        }
    }

    // =====================================================================
    //                            CORE APPLICATION LOGIC
    // =====================================================================

    /**
     * Secure login: choose OTP or biometric, then verify credential.
     * @throws Exception on authentication failure
     */
    void login() throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Choose auth (1=OTP,2=Bio): ");
        int choice = sc.nextInt(); 
        sc.nextLine();  // consume newline
        System.out.print("Enter credential: ");
        String cred = sc.nextLine();
        AuthenticationMethod auth = (choice == 1)
            ? new OTPAuthentication()
            : new BiometricAuthentication();
        if (!auth.authenticate(cred)) {
            throw new Exception("Authentication failed");
        }
    }

    /** Add a batch of transactions (varargs example). */
    void addTransactions(Transaction... txs) {
        for (Transaction t : txs) {
            try {
                t.process();
                transactions.add(t);
            } catch (Exception e) {
                sendAlert(e.getMessage());
            }
        }
    }

    /** Record a single income. */
    void addIncome(double amt, String cat) {
        addTransactions(new Income(amt, cat));
    }

    /** Record a single expense (throws if fails). */
    void addExpense(double amt, String cat) throws Exception {
        Expense e = new Expense(amt, cat);
        e.process();
        transactions.add(e);
        // update all goals (deduct expense as negative progress)
        for (Goal g : goals) {
            g.update(-amt);
        }
    }

    /** Set one or more budgets in one call (varargs). */
    void setBudgets(Budget... bs) {
        budgets.addAll(Arrays.asList(bs));
    }

    /** Create a single budget (overloaded). */
    void setBudget(String cat, double lim, Period p) {
        setBudgets(new Budget(cat, lim, p));
    }

    /** Set a batch of goals at once (varargs). */
    void setGoals(Goal... gs) {
        goals.addAll(Arrays.asList(gs));
    }

    /** Create a single goal (overloaded). */
    void setGoal(String name, double tgt) {
        setGoals(new Goal(name, tgt));
    }

    /** Schedule an automated recurring payment. */
    void scheduleRecurring(double amt, String cat, long delayMs, long periodMs) {
        timer.schedule(new RecurringPayment(amt, cat), delayMs, periodMs);
    }

    /** Show overall summary (balance, count, goals). */
    void showSummary() {
        // wrapper usage examples:
        Integer cnt    = Integer.valueOf(transactions.size());
        Double  balSum = Double.valueOf(balance);
        System.out.println("Balance: " + balSum + "; #Txns: " + cnt);
        for (Goal g : goals) {
            System.out.println("Goal: " + g.status() +
                (g.isAchieved() ? " (Achieved)" : ""));
        }
    }

    /** Show category-specific summary (overloaded). */
    void showSummary(String category) {
        double total = 0;
        for (Transaction t : transactions) {
            if (t.category.equals(category)) total += t.amount;
        }
        System.out.println("Total for " + category + ": " + total);
    }

    /** Default CSV export (no-arg overload). */
    void exportToCSV() throws IOException {
        exportToCSV("data.csv");
    }

    /** Exports all transactions to a CSV file. */
    public void exportToCSV(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println("Type,Amount,Category,Date");
            for (Transaction t : transactions) {
                pw.printf("%s,%.2f,%s,%s\n",
                    t.getClass().getSimpleName(),
                    t.amount,
                    t.category,
                    t.date
                );
            }
        }
        System.out.println("Data exported to " + filename);
    }

    /** Send an alert to the user (budget exceed, errors, etc.). */
    public void sendAlert(String message) {
        System.out.println("[ALERT] " + message);
    }

    /**
     * Main menu loop driving all features.
     * Captures user input and invokes appropriate methods.
     */
    void run() {
        try {
            login();  // secure login first
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            // display options
            System.out.println(
                "\n1=Income  2=Expense  3=Budget  4=Goal  5=Recurring\n" +
                "6=Summary 7=Export   8=Visualize 9=Exit"
            );

            int choice = sc.nextInt(); sc.nextLine();
            try {
                switch (choice) {
                    case 1:
                        System.out.print("Amount: ");
                        double a = sc.nextDouble(); sc.nextLine();
                        System.out.print("Category: ");
                        String c = sc.nextLine();
                        addIncome(a, c);
                        break;
                    case 2:
                        System.out.print("Amount: ");
                        a = sc.nextDouble(); sc.nextLine();
                        System.out.print("Category: ");
                        c = sc.nextLine();
                        addExpense(a, c);
                        break;
                    case 3:
                        System.out.print("Category: ");
                        String cat = sc.nextLine();
                        System.out.print("Limit: ");
                        double lim = sc.nextDouble(); sc.nextLine();
                        System.out.print("Period (MONTHLY, etc): ");
                        String p = sc.nextLine();
                        setBudget(cat, lim, Period.valueOf(p.toUpperCase()));
                        break;
                    case 4:
                        System.out.print("Goal name: ");
                        String n = sc.nextLine();
                        System.out.print("Target amount: ");
                        double t = sc.nextDouble(); sc.nextLine();
                        setGoal(n, t);
                        break;
                    case 5:
                        System.out.print("Amt: ");
                        a = sc.nextDouble(); sc.nextLine();
                        System.out.print("Category: ");
                        c = sc.nextLine();
                        scheduleRecurring(a, c, 0, 24*3600*1000);
                        break;
                    case 6:
                        showSummary();
                        break;
                    case 7:
                        exportToCSV();
                        break;
                    case 8:
                        DataVisualizer.generateCharts(transactions);
                        break;
                    case 9:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice");
                        break;
                }
            } catch (Exception e) {
                // catch any errors from process(), export, etc.
                sendAlert(e.getMessage());
            }
        }

        // on exit: encrypt & backup, then stop all timers
        CryptoUtil.encryptAndSave(transactions, "data.enc");
        CloudBackup.upload("data.enc");
        timer.cancel();
        sc.close();
    }

    /** Program entry point. */
    public static void main(String[] args) {
        new FinanceTracker().run();
    }
}


