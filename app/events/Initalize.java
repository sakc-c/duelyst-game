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
import structures.basic.Player;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

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
		gameState.setBoard(out, board);

		// Set Player 1 & 2 health
		BasicCommands.setPlayer1Health(out, player1);
		BasicCommands.setPlayer2Health(out, player2);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Error");
		}

		//set Attack and Health for both Avatars
		Unit player1Avatar = player1.getAvatar();
		BasicCommands.setUnitAttack(out, player1Avatar, 2);
		BasicCommands.setUnitHealth(out, player1Avatar, 20);
		player1Avatar.setCurrentHealth(20);
		player1Avatar.setAttackPower(2);

		Unit player2Avatar = player2.getAvatar();
		BasicCommands.setUnitAttack(out, player2Avatar, 2);
		BasicCommands.setUnitHealth(out, player2Avatar, 20);
		player2Avatar.setCurrentHealth(20);
		player2Avatar.setAttackPower(2);

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Error");
		}

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


