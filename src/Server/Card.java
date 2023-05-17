package Server;

public class Card { // 카드의 정보가 담길 클래스
    private Suit suit;
    private Rank rank;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Rank rank() {
        return rank;
    }

    public Suit suit() {
        return suit;
    }

    public String showCard() {
        return suit.getName() + rank.getName();
    }
}

enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
    NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");
    private final int rank;
    private final String name;

    Rank(int rank, String name) {
        this.rank = rank;
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

enum Suit {
    CLUBS("\u2663"), DIAMONDS("\u25C6"), HEARTS("\u2665"), SPADES("\u2660");

    private final String name;

    Suit(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}