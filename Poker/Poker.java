
import java.util.*;

enum Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES
}

enum Rank {
    TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6),
    SEVEN(7), EIGHT(8), NINE(9), TEN(10),
    JACK(11), QUEEN(12), KING(13), ACE(14);

    private final int value;

    Rank(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}

class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.remove(cards.size() - 1);
    }
}

enum HandRank {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH
}

class HandValue implements Comparable<HandValue> {
    private final HandRank rank;
    private final List<Integer> tieBreakers;

    public HandValue(HandRank rank, List<Integer> tieBreakers) {
        this.rank = rank;
        this.tieBreakers = new ArrayList<>(tieBreakers);
    }

    public HandRank getRank() {
        return rank;
    }

    @Override
    public int compareTo(HandValue other) {
        int result = Integer.compare(rank.ordinal(), other.rank.ordinal());

        if (result != 0) {
            return result;
        }

        for (int i = 0; i < Math.min(tieBreakers.size(), other.tieBreakers.size()); i++) {
            result = Integer.compare(tieBreakers.get(i), other.tieBreakers.get(i));
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        return rank + " " + tieBreakers;
    }
}

class HandEvaluator {

    public HandValue evaluate(List<Card> cards) {
        if (cards.size() != 5) {
            throw new IllegalArgumentException("Exactly five cards required");
        }

        List<Integer> values = cards.stream()
                .map(c -> c.getRank().getValue())
                .sorted(Comparator.reverseOrder())
                .toList();

        boolean flush = cards.stream()
                .allMatch(c -> c.getSuit() == cards.get(0).getSuit());

        boolean straight = isStraight(values);
        int straightHigh = getStraightHigh(values);

        Map<Integer, Integer> frequency = new HashMap<>();

        for (int value : values) {
            frequency.merge(value, 1, Integer::sum);
        }

        List<Map.Entry<Integer, Integer>> groups = new ArrayList<>(frequency.entrySet());

        groups.sort((a, b) -> {
            int comparison = Integer.compare(b.getValue(), a.getValue());
            return comparison != 0 ? comparison : Integer.compare(b.getKey(), a.getKey());
        });

        List<Integer> kickers = new ArrayList<>();

        for (Map.Entry<Integer, Integer> group : groups) {
            for (int i = 0; i < group.getValue(); i++) {
                kickers.add(group.getKey());
            }
        }

        if (flush && straight && straightHigh == 14) {
            return new HandValue(HandRank.ROYAL_FLUSH, List.of(14));
        }

        if (flush && straight) {
            return new HandValue(HandRank.STRAIGHT_FLUSH, List.of(straightHigh));
        }

        if (groups.get(0).getValue() == 4) {
            return new HandValue(HandRank.FOUR_OF_A_KIND, kickers);
        }

        if (groups.get(0).getValue() == 3 && groups.get(1).getValue() == 2) {
            return new HandValue(HandRank.FULL_HOUSE, kickers);
        }

        if (flush) {
            return new HandValue(HandRank.FLUSH, values);
        }

        if (straight) {
            return new HandValue(HandRank.STRAIGHT, List.of(straightHigh));
        }

        if (groups.get(0).getValue() == 3) {
            return new HandValue(HandRank.THREE_OF_A_KIND, kickers);
        }

        if (groups.get(0).getValue() == 2 && groups.get(1).getValue() == 2) {
            return new HandValue(HandRank.TWO_PAIR, kickers);
        }

        if (groups.get(0).getValue() == 2) {
            return new HandValue(HandRank.ONE_PAIR, kickers);
        }

        return new HandValue(HandRank.HIGH_CARD, values);
    }

    private boolean isStraight(List<Integer> values) {
        Set<Integer> unique = new HashSet<>(values);

        if (unique.size() != 5) {
            return false;
        }

        if (unique.containsAll(List.of(14, 2, 3, 4, 5))) {
            return true;
        }

        return values.get(0) - values.get(4) == 4;
    }

    private int getStraightHigh(List<Integer> values) {
        if (new HashSet<>(values).containsAll(List.of(14, 2, 3, 4, 5))) {
            return 5;
        }

        return values.get(0);
    }
}

class Player {
    private final String name;
    private int chips;
    private int contribution;
    private boolean folded;
    private final List<Card> hand = new ArrayList<>();

    public Player(String name, int chips) {
        this.name = name;
        this.chips = chips;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public boolean isFolded() {
        return folded;
    }

    public void payAnte(int ante) {
        if (chips < ante) {
            throw new IllegalStateException(name + " does not have enough chips");
        }

        chips -= ante;
        contribution += ante;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void fold() {
        folded = true;
    }

    public void receiveWinnings(int amount) {
        chips += amount;
    }

    public void reset() {
        hand.clear();
        folded = false;
        contribution = 0;
    }
}

class PokerGame {
    private final List<Player> players;
    private final Deck deck;
    private final HandEvaluator evaluator;
    private final int ante;
    private int pot;

    public PokerGame(List<Player> players, int ante) {
        if (players == null || players.size() < 2) {
            throw new IllegalArgumentException("At least two players required");
        }

        if (ante <= 0) {
            throw new IllegalArgumentException("Ante must be positive");
        }

        this.players = new ArrayList<>(players);
        this.ante = ante;
        this.deck = new Deck();
        this.evaluator = new HandEvaluator();
    }

    public void startGame() {
        pot = 0;

        for (Player player : players) {
            player.reset();
            player.payAnte(ante);
            pot += ante;
        }

        for (int i = 0; i < 5; i++) {
            for (Player player : players) {
                player.addCard(deck.draw());
            }
        }

        System.out.println("Cards distributed");
    }

    public void foldPlayer(Player player) {
        if (!players.contains(player)) {
            throw new IllegalArgumentException("Player does not belong to this game");
        }

        if (player.isFolded()) {
            return;
        }

        player.fold();
        System.out.println(player.getName() + " folded");
    }

    public void showDown() {
        List<Player> activePlayers = players.stream()
                .filter(player -> !player.isFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            System.out.println("No active players. Pot remains undistributed.");
            return;
        }

        if (activePlayers.size() == 1) {
            distributePot(List.of(activePlayers.get(0)));
            return;
        }

        Map<Player, HandValue> results = new LinkedHashMap<>();

        for (Player player : activePlayers) {
            HandValue value = evaluator.evaluate(player.getHand());
            results.put(player, value);

            System.out.println(player.getName() + " : " +
                    player.getHand() + " => " + value);
        }

        HandValue bestHand = Collections.max(results.values());

        List<Player> winners = new ArrayList<>();

        for (Map.Entry<Player, HandValue> entry : results.entrySet()) {
            if (entry.getValue().compareTo(bestHand) == 0) {
                winners.add(entry.getKey());
            }
        }

        distributePot(winners);
    }

    private void distributePot(List<Player> winners) {
        int share = pot / winners.size();
        int remainder = pot % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            int winnings = share + (i == 0 ? remainder : 0);
            winners.get(i).receiveWinnings(winnings);
        }

        System.out.println("Winners: " +
                winners.stream().map(Player::getName).toList());

        System.out.println("Pot distributed: " + pot);

        pot = 0;
    }
}

public class Main {
    public static void main(String[] args) {
        Player p1 = new Player("Alice", 1000);
        Player p2 = new Player("Bob", 1000);
        Player p3 = new Player("Charlie", 1000);

        PokerGame game = new PokerGame(
                Arrays.asList(p1, p2, p3), 100);

        game.startGame();

        System.out.println("Alice's cards: " + p1.getHand());
        System.out.println("Bob's cards: " + p2.getHand());
        System.out.println("Charlie's cards: " + p3.getHand());

        game.foldPlayer(p2);

        game.showDown();

        System.out.println("Final chip balances:");

        System.out.println(p1.getName() + ": " + p1.getChips());
        System.out.println(p2.getName() + ": " + p2.getChips());
        System.out.println(p3.getName() + ": " + p3.getChips());
    }
}