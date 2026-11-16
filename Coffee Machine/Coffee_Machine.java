
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

enum Ingredient {
    COFFEE,
    MILK,
    WATER,
    CHOCOLATE
}

enum DrinkType {
    ESPRESSO,
    LATTE,
    CAPPUCCINO
}

class Recipe {
    private final Map<Ingredient, Integer> ingredients;

    public Recipe(Map<Ingredient, Integer> ingredients) {
        this.ingredients = new EnumMap<>(ingredients);
    }

    public Map<Ingredient, Integer> getIngredients() {
        return Collections.unmodifiableMap(ingredients);
    }
}

abstract class Beverage {
    private final DrinkType drinkType;
    private final Recipe recipe;

    protected Beverage(DrinkType drinkType, Recipe recipe) {
        this.drinkType = drinkType;
        this.recipe = recipe;
    }

    public DrinkType getDrinkType() {
        return drinkType;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void serve() {
        System.out.println(drinkType + " is ready!");
    }
}

class Espresso extends Beverage {
    public Espresso(Recipe recipe) {
        super(DrinkType.ESPRESSO, recipe);
    }
}

class Latte extends Beverage {
    public Latte(Recipe recipe) {
        super(DrinkType.LATTE, recipe);
    }
}

class Cappuccino extends Beverage {
    public Cappuccino(Recipe recipe) {
        super(DrinkType.CAPPUCCINO, recipe);
    }
}

class CoffeeMachine {
    private final Map<Ingredient, Integer> inventory;
    private final Map<DrinkType, Recipe> recipes;
    private final ReentrantLock lock = new ReentrantLock();

    public CoffeeMachine() {
        inventory = new EnumMap<>(Ingredient.class);
        recipes = new EnumMap<>(DrinkType.class);

        initializeInventory();
        initializeRecipes();
    }

    private void initializeInventory() {
        inventory.put(Ingredient.COFFEE, 500);
        inventory.put(Ingredient.MILK, 500);
        inventory.put(Ingredient.WATER, 1000);
        inventory.put(Ingredient.CHOCOLATE, 200);
    }

    private void initializeRecipes() {
        recipes.put(
                DrinkType.ESPRESSO,
                new Recipe(Map.of(
                        Ingredient.COFFEE, 20,
                        Ingredient.WATER, 50
                ))
        );

        recipes.put(
                DrinkType.LATTE,
                new Recipe(Map.of(
                        Ingredient.COFFEE, 20,
                        Ingredient.MILK, 100,
                        Ingredient.WATER, 50
                ))
        );

        recipes.put(
                DrinkType.CAPPUCCINO,
                new Recipe(Map.of(
                        Ingredient.COFFEE, 20,
                        Ingredient.MILK, 60,
                        Ingredient.WATER, 50
                ))
        );
    }

    public Beverage makeDrink(DrinkType drinkType) {
        lock.lock();

        try {
            Recipe recipe = recipes.get(drinkType);

            if (recipe == null) {
                throw new IllegalArgumentException(
                        "Unsupported drink: " + drinkType
                );
            }

            validateIngredients(recipe);

            consumeIngredients(recipe);

            Beverage beverage = createBeverage(drinkType, recipe);

            beverage.serve();

            return beverage;

        } finally {
            lock.unlock();
        }
    }

    private void validateIngredients(Recipe recipe) {
        for (Map.Entry<Ingredient, Integer> entry :
                recipe.getIngredients().entrySet()) {

            Ingredient ingredient = entry.getKey();
            int required = entry.getValue();
            int available = inventory.getOrDefault(ingredient, 0);

            if (available < required) {
                throw new IllegalStateException(
                        "Insufficient " + ingredient +
                                ". Required: " + required +
                                ", Available: " + available
                );
            }
        }
    }

    private void consumeIngredients(Recipe recipe) {
        for (Map.Entry<Ingredient, Integer> entry :
                recipe.getIngredients().entrySet()) {

            Ingredient ingredient = entry.getKey();
            int required = entry.getValue();

            inventory.put(
                    ingredient,
                    inventory.get(ingredient) - required
            );
        }
    }

    private Beverage createBeverage(
            DrinkType drinkType,
            Recipe recipe
    ) {
        switch (drinkType) {
            case ESPRESSO:
                return new Espresso(recipe);

            case LATTE:
                return new Latte(recipe);

            case CAPPUCCINO:
                return new Cappuccino(recipe);

            default:
                throw new IllegalArgumentException(
                        "Unsupported drink: " + drinkType
                );
        }
    }

    public Map<Ingredient, Integer> getInventory() {
        lock.lock();

        try {
            return Collections.unmodifiableMap(
                    new EnumMap<>(inventory)
            );
        } finally {
            lock.unlock();
        }
    }

    public void refill(Ingredient ingredient, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be positive"
            );
        }

        lock.lock();

        try {
            inventory.put(
                    ingredient,
                    inventory.getOrDefault(ingredient, 0) + quantity
            );
        } finally {
            lock.unlock();
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {

        CoffeeMachine coffeeMachine = new CoffeeMachine();

        Runnable espressoRequest = () -> {
            try {
                coffeeMachine.makeDrink(DrinkType.ESPRESSO);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };

        Runnable latteRequest = () -> {
            try {
                coffeeMachine.makeDrink(DrinkType.LATTE);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };

        Thread t1 = new Thread(espressoRequest);
        Thread t2 = new Thread(latteRequest);
        Thread t3 = new Thread(espressoRequest);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println("Remaining Inventory:");
        System.out.println(coffeeMachine.getInventory());
    }
}