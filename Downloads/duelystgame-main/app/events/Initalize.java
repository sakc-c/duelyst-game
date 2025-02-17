package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.basic.Card;
import structures.basic.Player;
import utils.OrderedCardLoader;
import structures.GameState;
import structures.basic.Board;
import commands.BasicCommands;
import structures.HumanPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// hello this is a change
		
		gameState.gameInitalised = true;
		
		gameState.something = true;

		Board board = new Board(out); // initalizing the board
		gameState.setBoard(board); // Store the board in GameState

		
		
        // Initialise Players
		gameState.player1 = new Player(20, 2);
		gameState.player2 = new Player(20, 2);


		// Set Player 1 & 2 health
		BasicCommands.setPlayer1Health(out, gameState.player1);
		BasicCommands.setPlayer2Health(out, gameState.player2);

		try {
		  Thread.sleep(100);
		} catch (InterruptedException e) {
     		e.printStackTrace();
		}

		// Load decks for both players
		List<Card> player1Deck = OrderedCardLoader.getPlayer1Cards(1);
     		// Shuffle the deck
		Collections.shuffle(player1Deck);
		gameState.player1Deck = player1Deck;

		List<Card> player2Deck = OrderedCardLoader.getPlayer2Cards(1);
     		// Shuffle the deck
		Collections.shuffle(player2Deck);
		gameState.player2Deck = player2Deck;

		// Draw initial hand (3 cards) for Player 1
		for (int i = 0; i < 3; i++) {
			Card card = gameState.player1Deck.remove(0); // Take from the top of the deck
			gameState.player1Hand.add(card); // Add to hand
			drawCard(out, card, i + 1); // Display the card (hand position 1-3)
		}

    
		// User 1 makes a change
		//CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		//Loaders_2024_Check.test(out);
	}

}


