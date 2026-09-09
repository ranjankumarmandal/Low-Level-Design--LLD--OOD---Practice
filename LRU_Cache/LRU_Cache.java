import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

interface Cache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
}

class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

class LRUCache<K, V> implements Cache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;

    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }

        this.capacity = capacity;
        this.map = new HashMap<>();

        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);

        head.next = tail;
        tail.prev = head;
    }

    @Override
    public Optional<V> get(K key) {
        validateKey(key);

        Node<K, V> node = map.get(key);

        if (node == null) {
            return Optional.empty();
        }

        remove(node);
        addToFront(node);

        return Optional.of(node.value);
    }

    @Override
    public void put(K key, V value) {
        validateKey(key);
        validateValue(value);

        Node<K, V> node = map.get(key);

        if (node != null) {
            node.value = value;
            remove(node);
            addToFront(node);
            return;
        }

        node = new Node<>(key, value);
        map.put(key, node);
        addToFront(node);

        if (map.size() > capacity) {
            Node<K, V> lruNode = tail.prev;
            remove(lruNode);
            map.remove(lruNode.key);
        }
    }

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;

        head.next.prev = node;
        head.next = node;
    }

    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;

        node.prev = null;
        node.next = null;
    }

    private void validateKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not supported");
        }
    }

    private void validateValue(V value) {
        if (value == null) {
            throw new IllegalArgumentException("Null values are not supported");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Cache<Integer, String> cache = new LRUCache<>(3);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");

        System.out.println(cache.get(1));

        cache.put(4, "D");

        System.out.println(cache.get(2));
        System.out.println(cache.get(3));
        System.out.println(cache.get(4));
    }
}