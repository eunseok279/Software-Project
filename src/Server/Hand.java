package Server;

import java.util.*;

public class Hand {
      List<Card> ownCards = new ArrayList<>();

    public enum HandRank {
        ROYAL_FLUSH, STRAIGHT_FLUSH, FOUR_OF_A_KIND, FULL_HOUSE, FLUSH, STRAIGHT, THREE_OF_A_KIND, TWO_PAIR, ONE_PAIR, HIGH_CARD
    }

    public HandRank determineHandRank() {
        // 헬퍼 메소드를 사용하여 각 족보를 확인하세요.
        if (isRoyalFlush()) {
            return HandRank.ROYAL_FLUSH;
        } else if (isStraightFlush()) {
            return HandRank.STRAIGHT_FLUSH;
        } else if (isFourOfAKind()) {
            return HandRank.FOUR_OF_A_KIND;
        } else if (isFullHouse()) {
            return HandRank.FULL_HOUSE;
        } else if (isFlush()) {
            return HandRank.FLUSH;
        } else if (isStraight()) {
            return HandRank.STRAIGHT;
        } else if (isThreeOfAKind()) {
            return HandRank.THREE_OF_A_KIND;
        } else if (isTwoPair()) {
            return HandRank.TWO_PAIR;
        } else if (isOnePair()) {
            return HandRank.ONE_PAIR;
        } else {
            return HandRank.HIGH_CARD;
        }
    }

    private boolean isOnePair() {
        Map<Rank, Integer> rankCountMap = new HashMap<>();

        for (Card card : ownCards) {
            rankCountMap.put(card.rank(), rankCountMap.getOrDefault(card.rank(), 0) + 1); // 숫자가 있으면 +1 없으면 count 1
        }

        for (int count : rankCountMap.values()) {
            if (count == 2) {
                return true;
            }
        }

        return false;
    }

    private boolean isTwoPair() {
        Map<Rank, Integer> rankCountMap = new HashMap<>();

        for (Card card : ownCards) {
            rankCountMap.put(card.rank(), rankCountMap.getOrDefault(card.rank(), 0) + 1);
        }

        int twoCount = 0;
        for (int count : rankCountMap.values()) {
            if (count == 2) {
                twoCount++;
            }
        }
        return twoCount == 2;
    }


    private boolean isThreeOfAKind() {
        if (ownCards.size() <= 3) return false;
        Map<Rank, Integer> rankCountMap = new HashMap<>();

        for (Card card : ownCards) {
            rankCountMap.put(card.rank(), rankCountMap.getOrDefault(card.rank(), 0) + 1);
        }

        for (int count : rankCountMap.values()) {
            if (count == 3) {
                return true;
            }
        }

        return false;
    }

    private boolean isStraight() { // 5개가 연속된 숫자
        if (ownCards.size() < 5) return false;
        List<Card> straightCards = new ArrayList<>(ownCards);
        straightCards.sort(cardComparator);
        int consecutiveCards = 1;

        for (int i = 0; i < straightCards.size() - 1; i++) {
            if (straightCards.get(i).rank().ordinal() + 1 == straightCards.get(i + 1).rank().ordinal()) {
                consecutiveCards++;
                if (consecutiveCards == 5) {
                    return true;
                }
            } else if (straightCards.get(i).rank().ordinal() != straightCards.get(i + 1).rank().ordinal()) {
                consecutiveCards = 1;
            }
        }
        return false;
    }

    private boolean isFlush() {
        if (ownCards.size() < 5) return false;
        Map<Suit, Integer> suitCountMap = new HashMap<>();

        for (Card card : ownCards) {
            suitCountMap.put(card.suit(), suitCountMap.getOrDefault(card.suit(), 0) + 1);
        }

        for (int count : suitCountMap.values()) {
            if (count == 5) {
                return true;
            }
        }
        return false;
    }

    private boolean isFullHouse() {
        if (ownCards.size() < 5) return false;
        return isThreeOfAKind() && isOnePair();
    }

    private boolean isFourOfAKind() {
        if (ownCards.size() < 5) return false;
        Map<Rank, Integer> rankCountMap = new HashMap<>();

        for (Card card : ownCards) {
            rankCountMap.put(card.rank(), rankCountMap.getOrDefault(card.rank(), 0) + 1);
        }

        for (int count : rankCountMap.values()) {
            if (count == 4) {
                return true;
            }
        }

        return false;
    }

    private boolean isStraightFlush() {
        if (ownCards.size() < 5) return false;
        Map<Suit, List<Card>> suitCountMap = new HashMap<>();

        for (Card card : ownCards) {
            suitCountMap.putIfAbsent(card.suit(), new ArrayList<>());
            suitCountMap.get(card.suit()).add(card);
        }

        for (List<Card> sameSuitCard : suitCountMap.values()) {
            if (sameSuitCard.size() >= 5) {
                sameSuitCard.sort(cardComparator);
                int consecutiveCards = 1;

                for (int i = 0; i < sameSuitCard.size() - 1; i++) {
                    if (sameSuitCard.get(i).rank().ordinal() + 1 == sameSuitCard.get(i + 1).rank().ordinal()) {
                        consecutiveCards++;
                        if (consecutiveCards == 5) {
                            return true;
                        }
                    } else if (sameSuitCard.get(i).rank().ordinal() != sameSuitCard.get(i + 1).rank().ordinal()) {
                        consecutiveCards = 1;
                    }
                }
            }
        }
        return false;
    }

    private boolean isRoyalFlush() {
        if (ownCards.size() < 5) return false;
        Map<Suit, List<Card>> suitCountMap = new HashMap<>();

        for (Card card : ownCards) {
            suitCountMap.putIfAbsent(card.suit(), new ArrayList<>());
            suitCountMap.get(card.suit()).add(card);
        }
        List<Card> royalCards = new ArrayList<>();
        for (List<Card> sameSuitCard : suitCountMap.values()) {
            if (sameSuitCard.size() >= 5) {
                for (Card card : sameSuitCard) {
                    if (card.rank() == Rank.KING || card.rank() == Rank.JACK || card.rank() == Rank.QUEEN || card.rank() == Rank.TEN || card.rank() == Rank.ACE) {
                        royalCards.add(card);
                    }
                }
            }
        }
        return new HashSet<>(royalCards).containsAll(Arrays.asList(Rank.KING, Rank.JACK, Rank.QUEEN, Rank.TEN, Rank.ACE));
    }

    public void showPedigree() {
        System.out.println("Your pedigree >> " + this.determineHandRank());
    }

    public void showHand() {
        for (Card card : ownCards)
            System.out.println(card.showCard());
    }

    public List<Integer> getQuadsRank() {
        List<Integer> pairRanks = new ArrayList<>();
        Map<Integer, Integer> rankCounts = getRankCounts();

        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 4) {
                pairRanks.add(entry.getKey());
                break;
            }
        }
        return pairRanks;
    }

    public List<Integer> getThreePairRanks() {
        List<Integer> pairRanks = new ArrayList<>();
        Map<Integer, Integer> rankCounts = getRankCounts();

        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) {
                if (pairRanks.size() == 1) pairRanks.remove(0);
                pairRanks.add(entry.getKey());
            }

        }

        pairRanks.sort(Collections.reverseOrder());

        return pairRanks;
    }

    public List<Integer> getTwoPairRanks() {
        List<Integer> pairRanks = new ArrayList<>();
        Map<Integer, Integer> rankCounts = getRankCounts();

        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 2) {
                if (pairRanks.size() == 2) pairRanks.remove(0);
                pairRanks.add(entry.getKey());
            }
        }

        pairRanks.sort(Collections.reverseOrder());

        return pairRanks;
    }

    public List<Integer> getPairRanks() {
        List<Integer> pairRanks = new ArrayList<>();
        Map<Integer, Integer> rankCounts = getRankCounts();

        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 2) {
                if (pairRanks.size() == 1) pairRanks.remove(0);
                pairRanks.add(entry.getKey());
            }
        }
        return pairRanks;
    }

    public Card getHighestCardInCombination() {
        // 스트레이트, 플러쉬, 스트레이트 플러쉬에 따라 가장 높은 카드를 찾기 위한 메서드
        HandRank handRank = this.determineHandRank();
        Card highestCard;

        if (isFlush()) highestCard = findHighestCardInFlush();
        else if (isStraightFlush()) highestCard = findHighestCardInStraightFlush();
        else highestCard = findHighestCardInStraight();

        return highestCard;
    }

    private Card findHighestCardInStraight() {
        List<Card> straightCards = new ArrayList<>(ownCards);
        straightCards.sort(cardComparator);

        List<Card> list = new ArrayList<>();
        for (int i = 0; i < straightCards.size() - 1; i++) {
            if (straightCards.get(i).rank().ordinal() + 1 == straightCards.get(i + 1).rank().ordinal()) {
                list.add(straightCards.get(i));
            } else if (straightCards.get(i).rank().ordinal() != straightCards.get(i + 1).rank().ordinal()) {
                list.clear();
            }
        }
        Card highestCard = list.get(0);
        for (Card card : list)
            if (card.rank().ordinal() > highestCard.rank().ordinal()) highestCard = card;
        return highestCard;
    }

    private Card findHighestCardInFlush() {
        // 플러쉬 또는 스트레이트 플러쉬의 가장 높은 카드를 찾는 메서드
        Map<Suit, List<Card>> cardsBySuit = new HashMap<>();

        for (Card card : ownCards) {
            cardsBySuit.putIfAbsent(card.suit(), new ArrayList<>());
            cardsBySuit.get(card.suit()).add(card);
        }

        List<Card> flushCards = null;
        for (List<Card> cards : cardsBySuit.values()) {
            if (cards.size() >= 5) {
                flushCards = cards;
                break;
            }
        }

        assert flushCards != null;
        Card highestCard = flushCards.get(0);
        for (Card card : flushCards) {
            if (card.rank().ordinal() > highestCard.rank().ordinal()) {
                highestCard = card;
            }
        }
        return highestCard;
    }

    private Card findHighestCardInStraightFlush() {
        Map<Suit, List<Card>> cardsBySuit = new HashMap<>();

        for (Card card : ownCards) {
            cardsBySuit.putIfAbsent(card.suit(), new ArrayList<>());
            cardsBySuit.get(card.suit()).add(card);
        }

        List<Card> list = new ArrayList<>();
        for (List<Card> sameSuitCard : cardsBySuit.values()) {
            if (sameSuitCard.size() >= 5) {
                sameSuitCard.sort(cardComparator);
                for (int i = 0; i < sameSuitCard.size() - 1; i++) {
                    if (sameSuitCard.get(i).rank().ordinal() + 1 == sameSuitCard.get(i + 1).rank().ordinal()) {
                        list.add(sameSuitCard.get(i));
                    } else if (sameSuitCard.get(i).rank().ordinal() != sameSuitCard.get(i + 1).rank().ordinal()) {
                        list.clear();
                    }
                }
            }
        }
        Card highestCard = list.get(0);
        for (Card card : list)
            if (card.rank().ordinal() > highestCard.rank().ordinal()) highestCard = card;
        return highestCard;
    }


    private Map<Integer, Integer> getRankCounts() {
        Map<Integer, Integer> rankCounts = new HashMap<>();
        for (Card card : ownCards) {
            int rank = card.rank().ordinal();
            rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
        }
        return rankCounts;
    }

    public int getKickerRank(List<Integer> pairRanks) { // 키커 반환
        List<Integer> sortedRanks = getSortedRanksDescending();
        for (int rank : sortedRanks) {
            if (!pairRanks.contains(rank)) { // pair 없는 랭크 중 가장 높은 랭크를 반환
                return rank;
            }
        }
        throw new IllegalStateException("Kicker not found for Two Pair hand");
    }

    private List<Integer> getSortedRanksDescending() {
        List<Integer> ranks = new ArrayList<>();
        for (Card card : ownCards) {
            ranks.add(card.rank().ordinal());
        }
        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    public Card findHighestCardInAll() {
        Card highestCard = ownCards.get(0);
        for (Card card : ownCards) {
            if (card.rank().ordinal() > highestCard.rank().ordinal()) {
                highestCard = card;
            }
        }
        return highestCard;
    }

    Comparator<Card> cardComparator = (card1, card2) -> card2.rank().compareTo(card1.rank());
}