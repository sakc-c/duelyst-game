package structures;

import akka.actor.ActorRef;
import events.EndTurnClicked;
import structures.basic.Card;
import structures.basic.Player;

import java.util.ArrayList;
import java.util.List;
import utils.OrderedCardLoader;

public class AIController extends Player {
    private List<Card> hand;  // Cards that the AI can play
    private List<Card> deck;  // All available cards

    public AIController(int health, int mana) {
        super(health, mana);
        this.hand = new ArrayList<>();
        this.deck = OrderedCardLoader.getPlayer2Cards(1);
    }

    // The AI draws a card from its deck and adds it to the hand. No need to show on UI
    public void drawCard() {
        if (hand.size() < 6 && !deck.isEmpty()) { // Ensure there's space and deck is not empty
            Card newCard = deck.remove(0); // Draw the first card from the deck
            hand.add(newCard);
        }

    }

    public void playCard(ActorRef out, GameState gameState) {
        //implement AI logic for playing a card


        // Trigger the "End Turn" event after the AI plays its card
        EndTurnClicked endTurnEvent = new EndTurnClicked();
        endTurnEvent.processEvent(out, gameState, null);  // Passing 'null' since the AI isn't clicking, it's automatic
    }
}
