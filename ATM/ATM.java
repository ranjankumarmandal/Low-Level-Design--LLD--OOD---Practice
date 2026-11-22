
import java.util.*;

enum TransactionType {
    WITHDRAWAL,
    DEPOSIT,
    BALANCE_INQUIRY
}

enum TransactionStatus {
    SUCCESS,
    FAILED
}

enum ATMState {
    IDLE,
    CARD_INSERTED,
    AUTHENTICATED,
    TRANSACTION_IN_PROGRESS
}

class Card {
    private final String cardNumber;
    private final String pin;
    private final String accountNumber;

    public Card(String cardNumber, String pin, String accountNumber) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.accountNumber = accountNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean validatePin(String enteredPin) {
        return pin.equals(enteredPin);
    }
}

class Account {
    private final String accountNumber;
    private double balance;

    public Account(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void debit(double amount) {
        if (amount <= 0 || amount > balance) {
            throw new IllegalArgumentException("Invalid withdrawal amount");
        }
        balance -= amount;
    }

    public void credit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid deposit amount");
        }
        balance += amount;
    }
}

class Bank {
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Card> cards = new HashMap<>();

    public void addAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public void addCard(Card card) {
        if (!accounts.containsKey(card.getAccountNumber())) {
            throw new IllegalArgumentException("Account does not exist");
        }
        cards.put(card.getCardNumber(), card);
    }

    public boolean authenticate(Card card, String pin) {
        Card registeredCard = cards.get(card.getCardNumber());

        return registeredCard != null
                && registeredCard.validatePin(pin);
    }

    public Account getAccount(Card card) {
        Card registeredCard = cards.get(card.getCardNumber());

        if (registeredCard == null) {
            throw new IllegalArgumentException("Invalid card");
        }

        return accounts.get(registeredCard.getAccountNumber());
    }

    public void withdraw(Account account, double amount) {
        account.debit(amount);
    }

    public void deposit(Account account, double amount) {
        account.credit(amount);
    }

    public double checkBalance(Account account) {
        return account.getBalance();
    }
}

class CashInventory {
    private final Map<Integer, Integer> denominationCounts;

    public CashInventory(Map<Integer, Integer> initialCash) {
        denominationCounts = new TreeMap<>(initialCash);
    }

    public synchronized boolean canDispense(Map<Integer, Integer> requested) {
        for (Map.Entry<Integer, Integer> entry : requested.entrySet()) {
            int denomination = entry.getKey();
            int count = entry.getValue();

            if (count <= 0 || !denominationCounts.containsKey(denomination)
                    || denominationCounts.get(denomination) < count) {
                return false;
            }
        }

        return true;
    }

    public synchronized void dispense(Map<Integer, Integer> requested) {
        if (!canDispense(requested)) {
            throw new IllegalArgumentException("Insufficient ATM cash");
        }

        for (Map.Entry<Integer, Integer> entry : requested.entrySet()) {
            denominationCounts.put(
                    entry.getKey(),
                    denominationCounts.get(entry.getKey()) - entry.getValue()
            );
        }
    }

    public synchronized void acceptDeposit(Map<Integer, Integer> deposited) {
        for (Map.Entry<Integer, Integer> entry : deposited.entrySet()) {
            int denomination = entry.getKey();
            int count = entry.getValue();

            if (count <= 0 || !denominationCounts.containsKey(denomination)) {
                throw new IllegalArgumentException("Unsupported denomination");
            }
        }

        for (Map.Entry<Integer, Integer> entry : deposited.entrySet()) {
            denominationCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public synchronized int getTotalCash() {
        int total = 0;

        for (Map.Entry<Integer, Integer> entry : denominationCounts.entrySet()) {
            total += entry.getKey() * entry.getValue();
        }

        return total;
    }

    public synchronized Map<Integer, Integer> getInventory() {
        return Collections.unmodifiableMap(new TreeMap<>(denominationCounts));
    }
}

interface CashDispenser {
    void dispenseCash(Map<Integer, Integer> denominations);
}

interface CashAcceptor {
    void acceptCash(Map<Integer, Integer> denominations);
}

class ATMCashDispenser implements CashDispenser {
    private final CashInventory inventory;

    public ATMCashDispenser(CashInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void dispenseCash(Map<Integer, Integer> denominations) {
        inventory.dispense(denominations);
        System.out.println("Cash dispensed: " + denominations);
    }
}

class ATMCashAcceptor implements CashAcceptor {
    private final CashInventory inventory;

    public ATMCashAcceptor(CashInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void acceptCash(Map<Integer, Integer> denominations) {
        inventory.acceptDeposit(denominations);
        System.out.println("Cash accepted: " + denominations);
    }
}

class Transaction {
    private final String transactionId;
    private final TransactionType type;
    private final double amount;
    private TransactionStatus status;

    public Transaction(TransactionType type, double amount) {
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.amount = amount;
        this.status = TransactionStatus.FAILED;
    }

    public void markSuccess() {
        status = TransactionStatus.SUCCESS;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }
}

class TransactionService {
    private final Bank bank;
    private final CashDispenser cashDispenser;
    private final CashAcceptor cashAcceptor;

    public TransactionService(
            Bank bank,
            CashDispenser cashDispenser,
            CashAcceptor cashAcceptor) {
        this.bank = bank;
        this.cashDispenser = cashDispenser;
        this.cashAcceptor = cashAcceptor;
    }

    public Transaction withdraw(Account account, int amount,
                                Map<Integer, Integer> denominations) {
        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL, amount);

        int total = calculateAmount(denominations);

        if (total != amount) {
            throw new IllegalArgumentException("Denomination total mismatch");
        }

        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient account balance");
        }

        bank.withdraw(account, amount);
        cashDispenser.dispenseCash(denominations);

        transaction.markSuccess();
        return transaction;
    }

    public Transaction deposit(Account account,
                               Map<Integer, Integer> denominations) {
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT, calculateAmount(denominations));

        int amount = (int) transaction.getAmount();

        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid deposit amount");
        }

        cashAcceptor.acceptCash(denominations);
        bank.deposit(account, amount);

        transaction.markSuccess();
        return transaction;
    }

    public Transaction balanceInquiry(Account account) {
        double balance = bank.checkBalance(account);

        Transaction transaction = new Transaction(
                TransactionType.BALANCE_INQUIRY, balance);

        transaction.markSuccess();
        return transaction;
    }

    private int calculateAmount(Map<Integer, Integer> denominations) {
        int total = 0;

        for (Map.Entry<Integer, Integer> entry : denominations.entrySet()) {
            if (entry.getKey() <= 0 || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Invalid denomination");
            }

            total += entry.getKey() * entry.getValue();
        }

        return total;
    }
}

class ATM {
    private final String atmId;
    private final Bank bank;
    private final TransactionService transactionService;

    private ATMState state;
    private Card currentCard;
    private Account currentAccount;

    public ATM(String atmId, Bank bank,
               TransactionService transactionService) {
        this.atmId = atmId;
        this.bank = bank;
        this.transactionService = transactionService;
        this.state = ATMState.IDLE;
    }

    public void insertCard(Card card) {
        if (state != ATMState.IDLE) {
            throw new IllegalStateException("ATM is not available");
        }

        currentCard = card;
        state = ATMState.CARD_INSERTED;
        System.out.println("Card inserted");
    }

    public boolean authenticate(String pin) {
        if (state != ATMState.CARD_INSERTED) {
            throw new IllegalStateException("Insert card first");
        }

        if (!bank.authenticate(currentCard, pin)) {
            System.out.println("Invalid PIN");
            return false;
        }

        currentAccount = bank.getAccount(currentCard);
        state = ATMState.AUTHENTICATED;

        System.out.println("Authentication successful");
        return true;
    }

    public Transaction withdraw(int amount,
                                Map<Integer, Integer> denominations) {
        validateAuthentication();

        state = ATMState.TRANSACTION_IN_PROGRESS;

        try {
            Transaction transaction = transactionService.withdraw(
                    currentAccount, amount, denominations);

            System.out.println("Withdrawal successful");
            return transaction;
        } finally {
            state = ATMState.AUTHENTICATED;
        }
    }

    public Transaction deposit(Map<Integer, Integer> denominations) {
        validateAuthentication();

        state = ATMState.TRANSACTION_IN_PROGRESS;

        try {
            Transaction transaction = transactionService.deposit(
                    currentAccount, denominations);

            System.out.println("Deposit successful");
            return transaction;
        } finally {
            state = ATMState.AUTHENTICATED;
        }
    }

    public Transaction checkBalance() {
        validateAuthentication();

        return transactionService.balanceInquiry(currentAccount);
    }

    public void ejectCard() {
        if (state == ATMState.TRANSACTION_IN_PROGRESS) {
            throw new IllegalStateException("Transaction in progress");
        }

        currentCard = null;
        currentAccount = null;
        state = ATMState.IDLE;

        System.out.println("Card ejected");
    }

    private void validateAuthentication() {
        if (state != ATMState.AUTHENTICATED) {
            throw new IllegalStateException("Please authenticate first");
        }
    }

    public String getAtmId() {
        return atmId;
    }

    public ATMState getState() {
        return state;
    }
}

public class Main {
    public static void main(String[] args) {

        Bank bank = new Bank();

        Account account = new Account("ACC101", 10000);
        bank.addAccount(account);

        Card card = new Card("CARD101", "1234", "ACC101");
        bank.addCard(card);

        Map<Integer, Integer> initialCash = new HashMap<>();
        initialCash.put(500, 20);
        initialCash.put(200, 20);
        initialCash.put(100, 50);

        CashInventory inventory = new CashInventory(initialCash);

        CashDispenser dispenser = new ATMCashDispenser(inventory);
        CashAcceptor acceptor = new ATMCashAcceptor(inventory);

        TransactionService transactionService =
                new TransactionService(bank, dispenser, acceptor);

        ATM atm = new ATM("ATM001", bank, transactionService);

        atm.insertCard(card);

        if (atm.authenticate("1234")) {

            System.out.println("Initial Balance: " + atm.checkBalance().getAmount());

            Map<Integer, Integer> withdrawal = new HashMap<>();
            withdrawal.put(500, 2);
            withdrawal.put(200, 2);

            Transaction withdrawalTransaction =
                    atm.withdraw(1400, withdrawal);

            System.out.println("Withdrawal Transaction: "
                    + withdrawalTransaction.getStatus());

            Map<Integer, Integer> deposit = new HashMap<>();
            deposit.put(500, 2);
            deposit.put(100, 3);

            Transaction depositTransaction = atm.deposit(deposit);

            System.out.println("Deposit Transaction: "
                    + depositTransaction.getStatus());

            System.out.println("Final Balance: " + atm.checkBalance().getAmount());

            atm.ejectCard();
        }
    }
}