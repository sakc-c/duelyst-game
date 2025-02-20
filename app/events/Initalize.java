package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.AIController;
import structures.GameState;
import structures.HumanPlayer;
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
public class Initalize implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// Initialize players
		HumanPlayer player1 = new HumanPlayer(20, 2, out);
		AIController player2 = new AIController(20, 2, out);
		gameState.initializePlayers(player1, player2); //passing them to gameState

		// Initialize the board
		Board board = new Board(out);
		gameState.setBoard(board);

		// Set Player 1 & 2 health
		BasicCommands.setPlayer1Health(out, player1);
		BasicCommands.setPlayer2Health(out, player2);

		//set Attack and Health for both Avatars
		BasicCommands.setUnitAttack(out, player1.getAvatar(), 2);
		BasicCommands.setUnitHealth(out, player1.getAvatar(), 20);
		BasicCommands.setUnitAttack(out, player2.getAvatar(), 2);
		BasicCommands.setUnitHealth(out, player2.getAvatar(), 20);

		// set Mana to 2 at the start of the game
		player1.setMana(2);
		BasicCommands.setPlayer1Mana(out, player1);

		// Draw initial hands
		player1.drawInitialHand(out);
		player2.drawInitialHand();

		//set Avatar owners
		player1.getAvatar().setOwner(player1);
		player2.getAvatar().setOwner(player2);

		// Mark the game as initialized
		gameState.setGameInitialized(true);
	}
}


