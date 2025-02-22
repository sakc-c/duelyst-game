package events;


import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.HumanPlayer;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;

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
public class CardClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		
		
		int handPosition = message.get("position").asInt();
		HumanPlayer player = (HumanPlayer) gameState.getCurrentPlayer();
        List<Card> hand = player.getHand();
        
       

     // Clear the highlight of the previously selected card (if any)
        Card previouslySelectedCard = gameState.getSelectedCard();
        if (previouslySelectedCard != null) {
            int previousPosition = hand.indexOf(previouslySelectedCard) + 1; // Get the position of the previously selected card
            BasicCommands.drawCard(out, previouslySelectedCard, previousPosition, 0); // Clear highlight (mode = 0)
        }

        // Highlight the newly clicked card
        if (handPosition >= 1 && handPosition <= hand.size()) {
            Card clickedCard = hand.get(handPosition - 1); // Adjust for 0-based index
            BasicCommands.drawCard(out, clickedCard, handPosition, 1); // Highlight with mode = 1

            // Store the selected card in the GameState
            gameState.setSelectedCard(clickedCard);
            
         // Check if the clicked card is a creature card
            if (clickedCard.isCreature()) {
                // Highlight valid tiles for summoning
            	highlightValidSummonTiles(out, gameState, message);
            } else {
                // If the card is a spell, clear any existing tile highlights
                gameState.clearAllHighlights(out);
                // Optionally, handle spell-specific logic here (e.g., highlight tiles for spell targeting)
            }
        }
    }
	
	private void highlightValidSummonTiles(ActorRef out, GameState gameState, JsonNode message) {
		// Clear all previously highlighted tiles
        gameState.clearAllHighlights(out);

        // Get the player's avatar tile
        Tile avatarTile = getAvatarTile(gameState);
        if (avatarTile == null) return;

        int tilex = avatarTile.getTilex();
        int tiley = avatarTile.getTiley();

        // Define all 8 possible adjacent directions
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1}, // Top-left, Top, Top-right
            {0, -1},          {0, 1},  // Left,       Right
            {1, -1},  {1, 0}, {1, 1}   // Bottom-left, Bottom, Bottom-right
        };

        // Iterate through all directions
        for (int[] dir : directions) {
            int newX = tilex + dir[0];
            int newY = tiley + dir[1];

            // Check if the new coordinates are within the board bounds
            if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                Tile tile = gameState.getBoard().getTile(newX, newY);

                // Check if the tile is empty (no unit on it)
                if (gameState.getBoard().getUnitOnTile(tile) == null) {
                    // Highlight the tile
                    BasicCommands.drawTile(out, tile, 1); // Highlight with mode = 1
                    gameState.addHighlightedTile(tile); // Track highlighted tiles
                }
            }
        }
    }

    private Tile getAvatarTile(GameState gameState) {
        // Get the current player's avatar
        Unit avatar = gameState.getCurrentPlayerAvatar();
        if (avatar == null) return null;

        // Get the tile where the avatar is located
        return gameState.getBoard().getTile(avatar.getPosition().getTilex(), avatar.getPosition().getTiley());
    }

}
		
		
		// Clear all previously highlighted tiles
        /* gameState.clearAllHighlights(out);

        // Get the player's avatar position
        Unit avatar = gameState.getCurrentPlayerAvatar();
        if (avatar == null) return;

        Position avatarPosition = avatar.getPosition();
        int tilex = avatarPosition.getTilex();
        int tiley = avatarPosition.getTiley();

        // Define all 8 possible adjacent directions
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1}, // Top-left, Top, Top-right
            {0, -1},          {0, 1},  // Left,       Right
            {1, -1},  {1, 0}, {1, 1}   // Bottom-left, Bottom, Bottom-right
        };

        // Iterate through all directions
        for (int[] dir : directions) {
            int newX = tilex + dir[0];
            int newY = tiley + dir[1];

            // Check if the new coordinates are within the board bounds
            if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                Tile tile = gameState.getBoard().getTile(newX, newY);

                // Check if the tile is empty (no unit on it)
                if (gameState.getBoard().getUnitOnTile(tile) == null) {
                    // Highlight the tile
                    BasicCommands.drawTile(out, tile, 1); // Highlight with mode = 1
                    gameState.addHighlightedTile(tile); // Track highlighted tiles
                }
            }
        } */
 
	

		
		// Get the player's avatar
	    /* Unit avatar = gameState.getCurrentPlayer().getAvatar();
	    if (avatar == null) return;

	    // Get the CURRENT tile where the avatar is located
	    Tile avatarTile = gameState.getBoard().getTile(avatar.getPosition().getTilex(), avatar.getPosition().getTiley());
	    if (avatarTile == null) return;

	    // Get all adjacent tiles
	    List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(avatarTile);

	    // Highlight valid tiles
	    for (Tile tile : adjacentTiles) {
	        if (tile.getUnit() == null) { // Only highlight tiles without units
	            BasicCommands.drawTile(out, tile, 1); // Highlight with mode = 1
	            gameState.addHighlightedTile(tile); // Track highlighted tiles
	        }
	    }
	} */
	


