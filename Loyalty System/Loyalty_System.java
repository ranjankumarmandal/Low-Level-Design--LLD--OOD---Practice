
import java.util.*;

enum LoyaltyTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}

enum TransactionType {
    EARN,
    REDEEM
}

class Customer {
    private final String customerId;
    private final String name;
    private final LoyaltyAccount loyaltyAccount;

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
        this.loyaltyAccount = new LoyaltyAccount(customerId);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }
}

class PointTransaction {
    private final String transactionId;
    private final TransactionType type;
    private final int points;
    private final Date transactionDate;

    public PointTransaction(TransactionType type, int points) {
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.points = points;
        this.transactionDate = new Date();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public int getPoints() {
        return points;
    }

    public Date getTransactionDate() {
        return new Date(transactionDate.getTime());
    }
}

class InsufficientPointsException extends RuntimeException {
    public InsufficientPointsException(String message) {
        super(message);
    }
}

interface TierPolicy {
    LoyaltyTier calculateTier(int lifetimeEarnedPoints);
}

class StandardTierPolicy implements TierPolicy {

    @Override
    public LoyaltyTier calculateTier(int points) {
        if (points >= 10000) {
            return LoyaltyTier.PLATINUM;
        } else if (points >= 5000) {
            return LoyaltyTier.GOLD;
        } else if (points >= 1000) {
            return LoyaltyTier.SILVER;
        }
        return LoyaltyTier.BRONZE;
    }
}

class LoyaltyAccount {
    private final String customerId;
    private int currentBalance;
    private int lifetimeEarnedPoints;
    private LoyaltyTier tier;
    private final List<PointTransaction> transactions;

    public LoyaltyAccount(String customerId) {
        this.customerId = customerId;
        this.currentBalance = 0;
        this.lifetimeEarnedPoints = 0;
        this.tier = LoyaltyTier.BRONZE;
        this.transactions = new ArrayList<>();
    }

    public synchronized void earnPoints(int points, TierPolicy tierPolicy) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }

        currentBalance += points;
        lifetimeEarnedPoints += points;

        transactions.add(
                new PointTransaction(TransactionType.EARN, points)
        );

        tier = tierPolicy.calculateTier(lifetimeEarnedPoints);
    }

    public synchronized double redeemPoints(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }

        if (points > currentBalance) {
            throw new InsufficientPointsException(
                    "Insufficient points. Available: " + currentBalance
            );
        }

        currentBalance -= points;

        transactions.add(
                new PointTransaction(TransactionType.REDEEM, points)
        );

        return points / 100.0;
    }

    public int getCurrentBalance() {
        return currentBalance;
    }

    public int getLifetimeEarnedPoints() {
        return lifetimeEarnedPoints;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<PointTransaction> getTransactions() {
        return Collections.unmodifiableList(
                new ArrayList<>(transactions)
        );
    }
}

class LoyaltyProgram {
    private final Map<String, Customer> customers;
    private final TierPolicy tierPolicy;

    public LoyaltyProgram(TierPolicy tierPolicy) {
        this.customers = new HashMap<>();
        this.tierPolicy = tierPolicy;
    }

    public void registerCustomer(Customer customer) {
        if (customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException(
                    "Customer already registered"
            );
        }

        customers.put(customer.getCustomerId(), customer);
    }

    public void earnPoints(String customerId, int points) {
        Customer customer = getCustomer(customerId);

        customer.getLoyaltyAccount().earnPoints(
                points,
                tierPolicy
        );
    }

    public double redeemPoints(String customerId, int points) {
        Customer customer = getCustomer(customerId);

        return customer.getLoyaltyAccount().redeemPoints(points);
    }

    public Customer getCustomer(String customerId) {
        Customer customer = customers.get(customerId);

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer not found: " + customerId
            );
        }

        return customer;
    }
}

public class Main {

    public static void main(String[] args) {

        TierPolicy tierPolicy = new StandardTierPolicy();

        LoyaltyProgram loyaltyProgram =
                new LoyaltyProgram(tierPolicy);

        Customer customer =
                new Customer("C101", "John");

        loyaltyProgram.registerCustomer(customer);

        loyaltyProgram.earnPoints("C101", 6000);

        LoyaltyAccount account =
                customer.getLoyaltyAccount();

        System.out.println("Customer: " + customer.getName());
        System.out.println(
                "Lifetime Points: " +
                        account.getLifetimeEarnedPoints()
        );
        System.out.println(
                "Current Balance: " +
                        account.getCurrentBalance()
        );
        System.out.println(
                "Tier: " + account.getTier()
        );

        double discount =
                loyaltyProgram.redeemPoints("C101", 2000);

        System.out.println(
                "Discount: $" + discount
        );

        System.out.println(
                "Remaining Balance: " +
                        account.getCurrentBalance()
        );

        System.out.println(
                "Lifetime Points: " +
                        account.getLifetimeEarnedPoints()
        );

        System.out.println(
                "Tier After Redemption: " +
                        account.getTier()
        );

        try {
            loyaltyProgram.redeemPoints("C101", 10000);
        } catch (InsufficientPointsException e) {
            System.out.println(e.getMessage());
        }
    }
}