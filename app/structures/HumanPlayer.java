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
    private ActorRef out;

    public HumanPlayer(int health, int mana, ActorRef out) {
        super(health, mana);
        this.hand = new ArrayList<>();
        this.deck = OrderedCardLoader.getPlayer1Cards(1);
        this.out = out;
    }

    public List<Card> getHand() {
        return hand;
    }

    public Unit getAvatar() {
        return avatar;
    }

    public void setAvatar(Unit avatar) {
        this.avatar = avatar;
    }

    public void drawInitialHand(ActorRef out) {
        for (int i = 0; i < 3; i++) {
            drawCard(out);
        }
    }

    public void playCard(Card card, ActorRef out) {
        if (hand.contains(card) && getMana() >= card.getManacost()){//once we set mana cost for cards?
        int removedIndex = hand.indexOf(card); // Get card position

         // deduct mana
            setMana(getMana() - card.getManacost());
            BasicCommands.setPlayer1Mana(out,this); //update UI
            //setMana(getMana() - card.getManacost()); // Deduct mana

            BasicCommands.deleteCard(out, removedIndex + 1); // Remove from UI

            // Shift remaining cards left in UI
            for (int i = removedIndex+1; i < hand.size(); i++) {
                Card shiftedCard = hand.get(i);
                BasicCommands.deleteCard(out, i + 1); // Clear old position before redrawing
                BasicCommands.drawCard(out, shiftedCard, i, 0); // Draw at new position
            }

            hand.remove(card); // Remove from hand

            // Ensure last UI slot is cleared after shifting
            BasicCommands.deleteCard(out, hand.size() + 1);
        }
    }


    public void attack(Unit target) {
        // Implement attack selection
    }

    public void drawCard(ActorRef out) {
    if (hand.size() < 6) { // Check if there's space in hand
        if (!deck.isEmpty()) { // Check if deck is not empty
            Card newCard = deck.remove(0);  // Get and remove the first card from the deck
            hand.add(newCard); // Add card to the player's hand

            int nextIndex = hand.size(); // Correct UI index
            BasicCommands.drawCard(out, newCard, nextIndex, 0); // Update UI
        }
    }
}
}
