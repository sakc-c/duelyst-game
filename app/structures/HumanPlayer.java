package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

import java.util.ArrayList;
import java.util.List;

public class HumanPlayer extends Player {
    private List<Card> hand;
    private List<Card> deck;
    private Unit avatar;

    public HumanPlayer(int health, int mana) {
        super(health, mana);
        this.hand = new ArrayList<>();
        this.deck = OrderedCardLoader.getPlayer1Cards(1);
        this.avatar = null; //we'll load it in Initialize event handler
    }

    public void playCard(Card card, ActorRef out) {
        if (hand.contains(card) && getMana() >= card.getManacost()) {
            setMana(getMana() - card.getManacost()); // Deduct mana
            int removedIndex = hand.indexOf(card); // Get card position
            hand.remove(card); // Remove from hand

            BasicCommands.deleteCard(out, removedIndex + 1); //remove from UI

            try {
                Thread.sleep(1000); // Wait for a second to show the card removal
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Shift any cards which come after deleted card to the left
            for (int i = removedIndex; i < hand.size(); i++) {
                Card shiftedCard = hand.get(i);
                // Redraw the shifted card at its new position
                BasicCommands.drawCard(out, shiftedCard, i + 1, 0); // Redraw card at new position
            }

            //delete the last card after shifting
            BasicCommands.deleteCard(out, hand.size());

        }

        // Additional logic for placing the card on the board
    }

    public void attack(Unit target) {
        // Implement attack selection
    }

    public void drawCard(ActorRef out) {
        if (hand.size() < 6) { // Check if there's space in hand
            if (!deck.isEmpty()) { // Check if deck is not empty
                Card newCard = deck.remove(0);  // Get and remove the first card from the deck
                hand.add(newCard); // Add card to the player's hand

                int nextIndex = hand.indexOf(newCard) + 1; // Find next available index
                BasicCommands.drawCard(out, newCard, nextIndex, 0); // Update UI
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("No more cards left to draw!"); //if deck empty
            }
        } else {
            System.out.println("Hand is full!"); //if no space in hand
        }
    }
}
