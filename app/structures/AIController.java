package structures;

import akka.actor.ActorRef;
import events.EndTurnClicked;
import structures.basic.Card;
import structures.basic.Player;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import commands.BasicCommands; // Import the BasicCommands
import utils.StaticConfFiles;

public class AIController extends Player {
    private List<Card> deck;  // All available cards
    private int health; // AI's health
    private ActorRef out;

    public AIController(int health, int mana, ActorRef out) {
        super(health, mana);
        this.deck = OrderedCardLoader.getPlayer2Cards(1);
        this.health = health;
        this.out = out;
    }

    //Getter method to retrieve health of AI
    public int getHealth() {
        return health;
    }


    public void drawInitialHand() {
        for (int i = 0; i < 3; i++) {
            drawCard();
        }
    }

    // The AI draws a card from its deck and adds it to the hand. No need to show on UI
    public void drawCard() {
        if (getHand().size() < 6 && !deck.isEmpty()) { // Ensure there's space and deck is not empty
            Card newCard = deck.remove(0); // Draw the first card from the deck
            getHand().add(newCard);
        }

    }

    public void playCard(Card card, ActorRef out) {
        //implement AI logic for playing a card
        // For now, just display the AI's hand for testing
        //super.displayHand(out);
        if (getHand().contains(card)) {
            if (getMana() >= card.getManacost()) {

                int removedIndex = getHand().indexOf(card); // Get card position

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
                BasicCommands.setPlayer2Mana(out, this);
            }


            // Trigger the "End Turn" event after the AI plays its card
            //EndTurnClicked endTurnEvent = new EndTurnClicked();
            // endTurnEvent.processEvent(out, gameState, null);  // Passing 'null' since the AI isn't clicking, it's automatic
        }
    }
}
