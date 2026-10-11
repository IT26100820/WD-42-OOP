
package ordermanagement;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final OrderFileManager manager =
            new OrderFileManager();

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            System.out.println("\n=== ORDER MANAGEMENT ===");
            System.out.println("1. Create Order");
            System.out.println("2. View All Orders");
            System.out.println("3. Update Order Status");
            System.out.println("4. Cancel Order");
            System.out.println("5. Exit");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createOrder();
                    case "2" -> viewOrders();
                    case "3" -> updateStatus();
                    case "4" -> cancelOrder();
                    case "5" -> running = false;
                    default -> System.out.println(
                            "Invalid option. Try again."
                    );
                }
            } catch (IOException e) {
                System.out.println("File error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Order Management closed.");
        scanner.close();
    }

    private static void createOrder() throws IOException {
        int id = readInt("Enter Order ID: ");
        int customerId = readInt("Enter Customer ID: ");
        double amount = readAmount("Enter Total Amount: ");

        Order order = new Order(id, customerId, amount);
        manager.saveOrder(order);

        System.out.println("Order created successfully!");
    }

    private static void viewOrders() throws IOException {
        List<Order> orders = manager.loadOrders();

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        System.out.println("\n--- ALL ORDERS ---");

        for (Order order : orders) {
            System.out.println(
                    "Order ID: " + order.getOrderId()
                            + " | Customer ID: " + order.getCustomerId()
                            + " | Amount: Rs. " + order.getTotalAmount()
                            + " | Status: " + order.getStatus()
            );
        }
    }

    private static void updateStatus() throws IOException {
        int id = readInt("Enter Order ID: ");

        System.out.println("1. PENDING");
        System.out.println("2. CONFIRMED");
        System.out.println("3. SHIPPED");
        System.out.println("4. DELIVERED");
        System.out.print("Choose new status: ");

        String choice = scanner.nextLine().trim();

        String status = switch (choice) {
            case "1" -> "PENDING";
            case "2" -> "CONFIRMED";
            case "3" -> "SHIPPED";
            case "4" -> "DELIVERED";
            default -> null;
        };

        if (status == null) {
            System.out.println("Invalid status option.");
            return;
        }

        boolean updated = manager.updateStatus(id, status);

        System.out.println(updated
                ? "Order status updated!"
                : "Order not found or already cancelled.");
    }

    private static void cancelOrder() throws IOException {
        int id = readInt("Enter Order ID to cancel: ");

        boolean cancelled = manager.cancelOrder(id);

        System.out.println(cancelled
                ? "Order cancelled successfully."
                : "Order not found or already cancelled.");
    }

    private static int readInt(String message) {
        System.out.print(message);
        int value = Integer.parseInt(scanner.nextLine().trim());

        if (value <= 0) {
            throw new IllegalArgumentException(
                    "ID must be greater than zero."
            );
        }

        return value;
    }

    private static double readAmount(String message) {
        System.out.print(message);
        double amount =
                Double.parseDouble(scanner.nextLine().trim());

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be a positive number."
            );
        }

        return amount;
    }
}
