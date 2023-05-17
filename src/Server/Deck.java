package Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck { // 카드 드로우, 섞기
    private List<Card> cards;

    public Deck() {
        initCard();
    }

    public void initCard() {
        this.cards = new ArrayList<Card>();
        Suit[] suits = Suit.values();
        Rank[] ranks = Rank.values();

        for (Suit suit : suits) {
            for (Rank rank : ranks) {
                this.cards.add(new Card(rank, suit));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(this.cards);
    }

    public Card drawCard() {
        Card card = cards.get(0);
        cards.remove(0);
        return card;
    }

    public void print() {
        for (int i = 0; i < 52; i++) {
            Card card = cards.get(i);
            System.out.println(card.showCard());
        }
    }
}
