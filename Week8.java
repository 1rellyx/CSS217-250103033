interface Pizza {
    String getDescription();
    double getCost();
}

class PlainPizza implements Pizza {
    @Override
    public String getDescription() {
        return "Plain Dough & Sauce";
    }

    @Override
    public double getCost() {
        return 5.00;
    }
}

abstract class ToppingDecorator implements Pizza {
    protected final Pizza pizza;

    public ToppingDecorator(Pizza pizza) {
        this.pizza = pizza;
    }
}

class Cheese extends ToppingDecorator {
    public Cheese(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Cheese";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 2.00;
    }
}

class Olives extends ToppingDecorator {
    public Olives(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Olives";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 1.00;
    }
}

class Pineapple extends ToppingDecorator {
    public Pineapple(Pizza pizza) {
        super(pizza);
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + ", Pineapple";
    }

    @Override
    public double getCost() {
        return pizza.getCost() + 1.50;
    }
}

public class Main {
    public static void main(String[] args) {
        Pizza myPizza = new Pineapple(new Olives(new Cheese(new PlainPizza())));

        System.out.println("Description: " + myPizza.getDescription());
        System.out.printf("Total Cost: $%.2f%n", myPizza.getCost());
    }
}
