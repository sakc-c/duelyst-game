package events;


import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a card.
 * The event returns the position in the player's hand the card resides within.
 * 
 * { 
 *   messageType = “cardClicked”
 *   position = <hand index position [1-6]>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int handPosition = message.get("position").asInt();
		//Get the Player object, assuming player 1 from the GameState.
		Player player = gameState.getCurrentPlayer();

		//Validate that the player object exists
		if (player != null) {
			// Access the card from the player's hand using the handIndex
			Card clickedCard = player.getCard(handPosition);

			//Checking if the card is valid.
			// should notify error to the UI if card is null.
			if (clickedCard != null) {
				//Set the selected card in GameState
				gameState.setSelectedCard(clickedCard);
				System.out.println("Card selected: " + clickedCard.getCardname());
			} else {
				System.out.println("No card found at index: " + handPosition);
				//**Send an error message to the front-end**
			}
		}
	}
}