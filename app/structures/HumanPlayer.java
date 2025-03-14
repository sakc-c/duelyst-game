package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Card;
import structures.basic.Player;
import utils.OrderedCardLoader;
import java.util.Collections;
import java.util.List;

public class HumanPlayer extends Player {
    private final List<Card> deck;
    private final ActorRef out;

    public HumanPlayer(int health, int mana, ActorRef out) {
        super(health, mana);
        this.deck = OrderedCardLoader.getPlayer1Cards(2);
        Collections.shuffle(this.deck);
        this.out = out;
    }

    /**
     * Draws the initial hand for the player at the start of the game.
     * Draws three cards and displays them in the UI.
     *
     * @param gameState The current game state.
     */
    public void drawInitialHand(GameState gameState) {
        for (int i = 0; i < 3; i++) {
            drawCard(gameState);
        }
        super.displayHand(out); 
    }

    /**
     * Plays a card from the player's hand if they have enough mana.
     * - Removes the card from the hand and updates the UI.
     * - Shifts remaining cards in the UI to maintain order.
     * - Deducts the card's mana cost and updates the mana display.
     *
     * @param card      The card being played.
     * @param out       The ActorRef for sending UI updates.
     * @param gameState The current game state.
     */
    public void playCard(Card card, ActorRef out, GameState gameState) {
        if (getHand().contains(card)) {
            if (getMana() >= card.getManacost()) {

                int removedIndex = getHand().indexOf(card); // Get card position
                System.out.println("Index" + removedIndex);

                // Remove the card from the UI
                BasicCommands.deleteCard(out, removedIndex + 1);

                // Shift remaining cards left in the UI
                for (int i = removedIndex + 1; i < getHand().size(); i++) {
                    Card shiftedCard = getHand().get(i);
                    BasicCommands.deleteCard(out, i + 1); // Clear old position
                    BasicCommands.drawCard(out, shiftedCard, i, 0); // Draw at new position
                }
                // Remove the card from the hand
                getHand().remove(card);

                // Ensure the last UI slot is cleared after shifting
                BasicCommands.deleteCard(out, getHand().size() + 1);

                // Deduct mana
                setMana(getMana() - card.getManacost());
                BasicCommands.setPlayer1Mana(out, this);
            }
        }
    }

    /**
     * Draws a card from the deck and adds it to the player's hand.
     *
     * - Ensures the hand does not exceed the maximum size (6 cards).
     * - If no space is available, the drawn card is lost.
     * - If both the deck and hand are empty, triggers game over.
     *
     * @param gameState The current game state.
     */
    public void drawCard(GameState gameState) {
        if (getHand().size() < 6) {
            if (!deck.isEmpty()) {
                Card newCard = deck.remove(0);
                getHand().add(newCard);

                int nextIndex = getHand().size(); // Correct UI index
                BasicCommands.drawCard(out, newCard, nextIndex, 0);
            }
        } else if (!deck.isEmpty()) {
            deck.remove(0); //card is lost even if no space in hand
        }
        if (deck.isEmpty() && getHand().isEmpty()) { //if deck is empty and hand is empty too, game over
            BasicCommands.addPlayer1Notification(out, "Deck finished", 2);
            Player winner = gameState.getPlayer2();
            gameState.endGame(winner, out);
        }
    }
}
