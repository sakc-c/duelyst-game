package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.AIController;
import structures.GameState;
import structures.HumanPlayer;
import structures.basic.Player;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = “endTurnClicked”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.getHighlightedTiles().isEmpty()) {
			gameState.clearAllHighlights(out);
		}
		// Get the current player and opponent
		Player endTurnPlayer = gameState.getCurrentPlayer();
		Player startTurnPlayer = gameState.getOpponentPlayer();

		// Set unused mana to 0 for the player who ended turn
		endTurnPlayer.setMana(0);
		if (endTurnPlayer instanceof HumanPlayer) {
			BasicCommands.setPlayer1Mana(out, endTurnPlayer);
		} else if (endTurnPlayer instanceof AIController) {
			BasicCommands.setPlayer2Mana(out, endTurnPlayer);
		}
		try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

		// The player to start turn gets mana (turn + 1)
		int opponentMana = gameState.getCurrentTurn() + 1;
		if (opponentMana > 9) {
			opponentMana = 9;
		}
		startTurnPlayer.setMana(opponentMana);
		if (startTurnPlayer instanceof HumanPlayer) {
			BasicCommands.setPlayer1Mana(out, startTurnPlayer);
		} else if (startTurnPlayer instanceof AIController) {
			BasicCommands.setPlayer2Mana(out, startTurnPlayer);
		}
		try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

		// Switch to the opponent's turn in GameState
		gameState.nextTurn();

		// Draw a card for the player ending turn
		if (endTurnPlayer instanceof HumanPlayer) {
			BasicCommands.addPlayer1Notification(out, "AI's Turn", 1);
			//((AIController)startTurnPlayer).playCard(out, gameState);  // AI plays a card automatically and after that triggers end turn
			((AIController)startTurnPlayer).displayHand(out);
			((HumanPlayer)endTurnPlayer).drawCard();
		} else if (endTurnPlayer instanceof AIController) {
			((HumanPlayer)startTurnPlayer).displayHand(out);
			((AIController)endTurnPlayer).drawCard();  // AI draws a card automatically
			BasicCommands.addPlayer1Notification(out, "Your Turn", 1);
		}
	} 


}
