
package ordermanagement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrderFileManager {

    private final Path filePath = Path.of("orders.txt");

    // Read all orders from the file
    public List<Order> loadOrders() throws IOException {
        List<Order> orders = new ArrayList<>();

        if (!Files.exists(filePath)) {
            return orders;
        }

        List<String> lines =
                Files.readAllLines(filePath, StandardCharsets.UTF_8);

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            String[] data = line.split("\\|", -1);

            if (data.length != 4) {
                System.out.println("Skipping invalid record: " + line);
                continue;
            }

            try {
                int id = Integer.parseInt(data[0]);
                int customerId = Integer.parseInt(data[1]);
                double amount = Double.parseDouble(data[2]);

                Order order = new Order(id, customerId, amount);
                order.setStatus(data[3]);
                orders.add(order);

            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid record: " + line);
            }
        }

        return orders;
    }

    // Save an order only if its ID is unique
    public void saveOrder(Order newOrder) throws IOException {
        List<Order> orders = loadOrders();

        for (Order existing : orders) {
            if (existing.getOrderId() == newOrder.getOrderId()) {
                throw new IllegalArgumentException(
                        "Order ID already exists: "
                                + newOrder.getOrderId()
                );
            }
        }

        orders.add(newOrder);
        writeOrders(orders);
    }

    // Update the status of an existing order
    public boolean updateStatus(int orderId, String newStatus)
            throws IOException {

        List<Order> orders = loadOrders();

        for (Order order : orders) {
            if (order.getOrderId() == orderId) {
                if ("CANCELLED".equals(order.getStatus())) {
                    return false;
                }

                order.setStatus(newStatus);
                writeOrders(orders);
                return true;
            }
        }

        return false;
    }

    // Cancel an order without deleting its history
    public boolean cancelOrder(int orderId) throws IOException {
        List<Order> orders = loadOrders();

        for (Order order : orders) {
            if (order.getOrderId() == orderId) {
                if ("CANCELLED".equals(order.getStatus())) {
                    return false;
                }

                order.setStatus("CANCELLED");
                writeOrders(orders);
                return true;
            }
        }

        return false;
    }

    // Rewrite the file with the latest order list
    private void writeOrders(List<Order> orders) throws IOException {
        List<String> lines = new ArrayList<>();

        for (Order order : orders) {
            lines.add(
                    order.getOrderId() + "|"
                            + order.getCustomerId() + "|"
                            + order.getTotalAmount() + "|"
                            + order.getStatus()
            );
        }

        Files.write(
                filePath,
                lines,
                StandardCharsets.UTF_8
        );
    }
}
