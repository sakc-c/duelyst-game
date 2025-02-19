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
		// Get the current player and opponent
		Player currentPlayer = gameState.getCurrentPlayer();
		Player opponentPlayer = gameState.getOpponentPlayer();

		// 1. Set unused mana to 0 for current player
		currentPlayer.setMana(0);
		BasicCommands.setPlayer1Mana(out, currentPlayer);

		// 2. Draw a card for the current player
		if (currentPlayer instanceof HumanPlayer) {
			HumanPlayer humanPlayer = (HumanPlayer) currentPlayer;
			humanPlayer.drawCard(out);
			BasicCommands.addPlayer1Notification(out, "AI's Turn", 1);
		} else if (currentPlayer instanceof AIController) {
			AIController aiPlayer = (AIController) currentPlayer;
			aiPlayer.drawCard();  // AI draws a card automatically
			aiPlayer.playCard(out, gameState);  // AI plays a card automatically based on logic on ends turn
			BasicCommands.addPlayer1Notification(out, "Your Turn", 1);
		}

		// 3. The opponent gets mana (turn + 1)
		int opponentMana = gameState.getCurrentTurn() + 1;
		opponentPlayer.setMana(opponentMana);
		BasicCommands.setPlayer2Mana(out, opponentPlayer);

		// 4. Switch to the opponent's turn in GameState
		gameState.nextTurn();
	}


}
