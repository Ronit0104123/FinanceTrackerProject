// File: FinanceTracker.java
package com.financetracker;

// Core imports
import java.util.*;     
import java.io.*; 


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class FinanceTracker implements Exportable, Notification {


    private double balance = 0;                              
    private List<Transaction> transactions = new ArrayList<>(); 
    private List<Budget> budgets = new ArrayList<>();  
    private List<Goal> goals = new ArrayList<>();  
    private Timer timer = new Timer();      

   
    
    abstract class Transaction {
        double amount;  
        String category; 
        Date date;       

        Transaction(double amount, String category) {
            this.amount = amount;
            this.category = category;
            this.date = new Date();
        }

        abstract void process() throws Exception;
    }


    class Income extends Transaction {
        Income(double amt, String cat) { super(amt, cat); }
        @Override
        void process() {
            balance += amount;  // add to balance
        }
    }

    class Expense extends Transaction {
        Expense(double amt, String cat) { super(amt, cat); }

        @Override
        void process() throws Exception {
            if (amount > balance) {
                throw new Exception("Insufficient funds");
            }
            balance -= amount;

            for (Budget b : budgets) {
                if (b.category.equals(category) && amount > b.limit) {
                    throw new Exception("Budget exceeded for " + category);
                }
            }
        }
    }


    class Budget {
        String category; 
        double limit;    
        Period period;   

        /** Standard constructor. */
        Budget(String category, double limit, Period period) {
            this.category = category;
            this.limit = limit;
            this.period = period;
        }
    }

    enum Period { FORTNIGHTLY, MONTHLY, TRIMESTER }


    class Goal {
        String name;   
        double target;  
        double progress;

        Goal(String name, double target) {
            this.name = name;
            this.target = target;
            this.progress = 0;
        }

        void update(double amt) {
            progress += amt;
        }

        boolean isAchieved() {
            return progress >= target;
        }

        String status() {
            return name + ": " + progress + "/" + target;
        }
    }

    class RecurringPayment extends TimerTask {
        double amount;
        String category;

        RecurringPayment(double amt, String cat) {
            this.amount   = amt;
            this.category = cat;
        }

        public void run() {
            try {
                addExpense(amount, category);
            } catch (Exception e) {
                sendAlert(e.getMessage());
            }
        }
    }

    static class CryptoUtil {
        static void encryptAndSave(Object data, String filename) {

        }
    }

    static class CloudBackup {
        static void upload(String filename) {
            System.out.println("Backing up " + filename);
        }
    }

    static class DataVisualizer {
  
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

    void addIncome(double amt, String cat) {
        addTransactions(new Income(amt, cat));
    }

    
    void addExpense(double amt, String cat) throws Exception {
        Expense e = new Expense(amt, cat);
        e.process();
        transactions.add(e);
        for (Goal g : goals) {
            g.update(-amt);
        }
    }

    void setBudgets(Budget... bs) {
        budgets.addAll(Arrays.asList(bs));
    }

    void setBudget(String cat, double lim, Period p) {
        setBudgets(new Budget(cat, lim, p));
    }

    void setGoals(Goal... gs) {
        goals.addAll(Arrays.asList(gs));
    }

    void setGoal(String name, double tgt) {
        setGoals(new Goal(name, tgt));
    }

    void scheduleRecurring(double amt, String cat, long delayMs, long periodMs) {
        timer.schedule(new RecurringPayment(amt, cat), delayMs, periodMs);
    }

    void showSummary() {
        Integer cnt    = Integer.valueOf(transactions.size());
        Double  balSum = Double.valueOf(balance);
        System.out.println("Balance: " + balSum + "; #Txns: " + cnt);
        for (Goal g : goals) {
            System.out.println("Goal: " + g.status() +
                (g.isAchieved() ? " (Achieved)" : ""));
        }
    }

    void showSummary(String category) {
        double total = 0;
        for (Transaction t : transactions) {
            if (t.category.equals(category)) total += t.amount;
        }
        System.out.println("Total for " + category + ": " + total);
    }

    void exportToCSV() throws IOException {
        exportToCSV("data.csv");
    }

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

    public void sendAlert(String message) {
        System.out.println("[ALERT] " + message);
    }

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
                sendAlert(e.getMessage());
            }
        }

        CryptoUtil.encryptAndSave(transactions, "data.enc");
        CloudBackup.upload("data.enc");
        timer.cancel();
        sc.close();
    }
    public static void main(String[] args) {
        new FinanceTracker().run();
    }
}


