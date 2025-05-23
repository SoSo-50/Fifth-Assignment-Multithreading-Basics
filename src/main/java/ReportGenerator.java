import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    private static final DecimalFormat DF = new DecimalFormat("0.00");

    static class TaskRunnable implements Runnable {
        private final String path;
        private double totalCost;
        private int totalAmount;
        private double totalDiscountAmount;
        private double highestCostAfterDiscount;
        private Product mostExpensiveProductInOrder;

        public TaskRunnable(String path) {
            this.path = path;
            this.totalCost = 0;
            this.totalAmount = 0;
            this.totalDiscountAmount = 0;
            this.highestCostAfterDiscount = 0;
            this.mostExpensiveProductInOrder = null;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getClassLoader().getResourceAsStream(path)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        int productId = Integer.parseInt(parts[0].trim());
                        int amount = Integer.parseInt(parts[1].trim());
                        double discountAmount = Double.parseDouble(parts[2].trim());

                        Product product = productCatalog[productId - 1];

                        if (product != null) {
                            double itemCost = product.getPrice() * amount;
                            double discountedCost = itemCost - discountAmount;

                            totalCost += discountedCost;
                            totalAmount += amount;
                            totalDiscountAmount += discountAmount;

                            if (discountedCost > highestCostAfterDiscount) {
                                highestCostAfterDiscount = discountedCost;
                                mostExpensiveProductInOrder = product;
                            }
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error processing file " + path + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void makeReport() {
            System.out.println("\n--- Report for " + path + " ---");
            System.out.println("Total cost: $" + DF.format(totalCost));
            System.out.println("Total items bought: " + totalAmount);

            double averageDiscount = (totalAmount > 0) ? totalDiscountAmount / totalAmount : 0;
            System.out.println("Average discount: $" + DF.format(averageDiscount));

            if (mostExpensiveProductInOrder != null) {
                System.out.println("Most expensive purchase after discount: " + mostExpensiveProductInOrder.getProductName() + " ($" + DF.format(highestCostAfterDiscount) + ")");
            } else {
                System.out.println("No expensive purchase recorded.");
            }
        }
    }

    static class Product {
        private int productID;
        private String productName;
        private double price;

        public Product(int productID, String productName, double price) {
            this.productID = productID;
            this.productName = productName;
            this.price = price;
        }

        public int getProductID() {
            return productID;
        }

        public String getProductName() {
            return productName;
        }

        public double getPrice() {
            return price;
        }
    }

    private static final String[] ORDER_FILES = {
            "2021_order_details.txt",
            "2022_order_details.txt",
            "2023_order_details.txt",
            "2024_order_details.txt"
    };

    static Product[] productCatalog = new Product[100];

    public static void loadProducts() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ReportGenerator.class.getClassLoader().getResourceAsStream("Products.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int productId = Integer.parseInt(parts[0].trim());
                    String productName = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());
                    if (productId > 0 && productId <= productCatalog.length) {
                        productCatalog[productId - 1] = new Product(productId, productName, price);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            loadProducts();

            List<Thread> threads = new ArrayList<>();
            List<TaskRunnable> tasks = new ArrayList<>();

            for (String filePath : ORDER_FILES) {
                TaskRunnable task = new TaskRunnable(filePath);
                Thread thread = new Thread(task);
                threads.add(thread);
                tasks.add(task);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            for (TaskRunnable task : tasks) {
                task.makeReport();
            }

        } catch (IOException e) {
            System.err.println("Error loading products or processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}