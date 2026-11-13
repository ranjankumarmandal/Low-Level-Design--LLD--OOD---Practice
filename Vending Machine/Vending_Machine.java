import java.util.*;

class Product {
    private final String id;
    private final String name;
    private final int priceInCents;

    public Product(String id, String name, int priceInCents) {
        if (priceInCents <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.id = id;
        this.name = name;
        this.priceInCents = priceInCents;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriceInCents() {
        return priceInCents;
    }
}

class Slot {
    private final String slotId;
    private Product product;
    private int quantity;

    public Slot(String slotId) {
        this.slotId = slotId;
        this.quantity = 0;
    }

    public synchronized void restock(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid restock details");
        }

        if (this.product != null && !this.product.getId().equals(product.getId())) {
            throw new IllegalStateException("Slot contains a different product");
        }

        this.product = product;
        this.quantity += quantity;
    }

    public synchronized Product getProduct() {
        return product;
    }

    public synchronized int getQuantity() {
        return quantity;
    }

    public String getSlotId() {
        return slotId;
    }

    public synchronized boolean isAvailable() {
        return product != null && quantity > 0;
    }

    public synchronized void dispense() {
        if (!isAvailable()) {
            throw new IllegalStateException("Product unavailable");
        }
        quantity--;
    }
}

class Receipt {
    private final String transactionId;
    private final String productName;
    private final int amountPaid;
    private final int productPrice;
    private final int changeReturned;

    public Receipt(String transactionId, String productName, int amountPaid,
                   int productPrice, int changeReturned) {
        this.transactionId = transactionId;
        this.productName = productName;
        this.amountPaid = amountPaid;
        this.productPrice = productPrice;
        this.changeReturned = changeReturned;
    }

    public void printReceipt() {
        System.out.println("\n----- RECEIPT -----");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Product: " + productName);
        System.out.println("Amount Paid: " + amountPaid + " cents");
        System.out.println("Product Price: " + productPrice + " cents");
        System.out.println("Change Returned: " + changeReturned + " cents");
        System.out.println("-------------------");
    }
}

class VendingMachine {
    private final Map<String, Slot> slots;

    public VendingMachine() {
        this.slots = new HashMap<>();
    }

    public synchronized void addSlot(Slot slot) {
        if (slot == null || slots.containsKey(slot.getSlotId())) {
            throw new IllegalArgumentException("Invalid or duplicate slot");
        }
        slots.put(slot.getSlotId(), slot);
    }

    public synchronized void restock(String slotId, Product product, int quantity) {
        Slot slot = slots.get(slotId);

        if (slot == null) {
            throw new IllegalArgumentException("Slot does not exist");
        }

        slot.restock(product, quantity);
    }

    public synchronized Receipt purchase(String slotId, int amountPaidInCents) {
        if (amountPaidInCents < 0) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        Slot slot = slots.get(slotId);

        if (slot == null) {
            throw new IllegalArgumentException("Invalid slot");
        }

        if (!slot.isAvailable()) {
            throw new IllegalStateException("Product out of stock");
        }

        Product product = slot.getProduct();
        int price = product.getPriceInCents();

        if (amountPaidInCents < price) {
            throw new IllegalArgumentException(
                    "Insufficient payment. Remaining amount: " +
                            (price - amountPaidInCents) + " cents"
            );
        }

        int change = amountPaidInCents - price;

        slot.dispense();

        return new Receipt(
                UUID.randomUUID().toString(),
                product.getName(),
                amountPaidInCents,
                price,
                change
        );
    }

    public synchronized void displayInventory() {
        System.out.println("\n----- INVENTORY -----");

        for (Slot slot : slots.values()) {
            Product product = slot.getProduct();

            if (product == null) {
                System.out.println(slot.getSlotId() + " : Empty");
            } else {
                System.out.println(
                        slot.getSlotId() + " : " +
                                product.getName() +
                                " | Price: " + product.getPriceInCents() +
                                " cents | Quantity: " + slot.getQuantity()
                );
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine();

        Product coke = new Product("P1", "Coke", 250);
        Product pepsi = new Product("P2", "Pepsi", 200);
        Product chips = new Product("P3", "Chips", 150);

        machine.addSlot(new Slot("A1"));
        machine.addSlot(new Slot("A2"));
        machine.addSlot(new Slot("B1"));

        machine.restock("A1", coke, 5);
        machine.restock("A2", pepsi, 3);
        machine.restock("B1", chips, 10);

        machine.displayInventory();

        try {
            Receipt receipt = machine.purchase("A1", 500);
            receipt.printReceipt();
        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }

        machine.displayInventory();

        try {
            Receipt receipt = machine.purchase("B1", 100);
            receipt.printReceipt();
        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }
}