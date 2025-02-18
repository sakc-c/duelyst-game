package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.Board;
import commands.BasicCommands;

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
	    gameState.player1 = new HumanPlayer(20, 2);
	    gameState.player2 = new AIController(20, 2);

	    // Initialise hands
	    gameState.player1Hand = new ArrayList<>();
	    gameState.player2Hand = new ArrayList<>();

	    // Set Player 1 & 2 health
	    BasicCommands.setPlayer1Health(out, gameState.player1);
	    BasicCommands.setPlayer2Health(out, gameState.player2);

	    // Draw initial hands
	    drawInitialHands(out, gameState);

		// User 1 makes a change
		//CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		//Loaders_2024_Check.test(out);
	}
	
	private void drawInitialHands(ActorRef out, GameState gameState) {
	    for (int i = 0; i < 3; i++) {
	        drawCardForPlayer(out, gameState, gameState.player1); // Initialise the hands of the HumanPlayer with 3 cards
	        drawCardForPlayer(out, gameState, gameState.player2); // Initialise the hands of the AIController with 3 cards
	    }
	}

	private void drawCardForPlayer(ActorRef out, GameState gameState, Player player) {
	    player.drawCard(out); // Calls the drawCard() method on the HumanPlayer and AIController classes
	}

}


