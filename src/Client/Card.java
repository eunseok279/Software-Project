package Client;

public record Card(Rank rank, Suit suit)  { // 카드의 정보가 담길 클래스

    public String showCard() {
        return suit.getName() + rank.getName();
    }
}

enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
    NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");
    private final String name;

    Rank(int rank, String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

enum Suit {
    CLUBS("C"), DIAMONDS("D"), HEARTS("H"), SPADES("S");

    private final String name;

    Suit(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}