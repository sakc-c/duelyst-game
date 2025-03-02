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

    public void playCard(ActorRef out, GameState gameState) {
        //implement AI logic for playing a card
        // For now, just display the AI's hand for testing
        super.displayHand(out);


        // Trigger the "End Turn" event after the AI plays its card
        //EndTurnClicked endTurnEvent = new EndTurnClicked();
       // endTurnEvent.processEvent(out, gameState, null);  // Passing 'null' since the AI isn't clicking, it's automatic
    }
}
