package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.Card;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Handles the logic for the Wraithling Swarm spell card.
 * Highlights all empty tiles on the board and summons three Wraithlings in sequence.
 */
public class WraithlingSwarm implements SpellEffect {

	@Override
	public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
	    // Clear all existing highlights
	    gameState.clearAllHighlights(out);

	    // Iterate through all tiles on the board
	    for (int x = 0; x < 9; x++) {
	        for (int y = 0; y < 5; y++) {
	            Tile currentTile = gameState.getBoard().getTile(x, y);

	            // Check if the current tile is empty
	            if (gameState.getBoard().getUnitOnTile(currentTile) == null) {
	                // Check if there are three consecutive vertical empty tiles below this tile
	                if (hasThreeVerticalEmptyTiles(gameState, currentTile)) {
	                    // Highlight the starting tile
	                    BasicCommands.drawTile(out, currentTile, 1); // Highlight mode = 1 (Blue)
	                    gameState.addHighlightedTile(currentTile); // Track highlighted tiles
	                    try {
	                        Thread.sleep(10); // Small delay to avoid overwhelming the front-end
	                    } catch (InterruptedException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        }
	    }
	}

	/**
	 * Checks if a tile has three consecutive vertical empty tiles below it.
	 *
	 * @param gameState The current game state.
	 * @param tile The tile to check.
	 * @return True if there are three consecutive vertical empty tiles below, false otherwise.
	 */
	private boolean hasThreeVerticalEmptyTiles(GameState gameState, Tile tile) {
	    int tileX = tile.getTilex();
	    int tileY = tile.getTiley();

	    // Check the next three tiles vertically (below the current tile)
	    for (int i = 0; i < 3; i++) {
	        int newY = tileY + i;

	        // Check if the new Y coordinate is within the board bounds
	        if (newY >= 5) {
	            return false; // Out of bounds
	        }

	        Tile verticalTile = gameState.getBoard().getTile(tileX, newY);

	        // Check if the tile is empty
	        if (gameState.getBoard().getUnitOnTile(verticalTile) != null) {
	            return false; // Tile is not empty
	        }
	    }

	    return true; // All three vertical tiles are empty
	}


	@Override
	public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
	    // Check if the target tile is valid (empty and highlighted)
	    if (gameState.getBoard().getUnitOnTile(targetTile) != null || !gameState.isHighlightedTile(targetTile)) {
	        
	        return;
	    }

	    int tileX = targetTile.getTilex();
	    int tileY = targetTile.getTiley();

	    // Summon 3 Wraithlings in the three consecutive vertical tiles below the target tile
	    for (int i = 0; i < 3; i++) {
	        int newY = tileY + i;

	        // Check if the new Y coordinate is within the board bounds
	        if (newY >= 5) {
	            
	            break;
	        }

	        Tile verticalTile = gameState.getBoard().getTile(tileX, newY);

	        // Check if the tile is empty
	        if (gameState.getBoard().getUnitOnTile(verticalTile) != null) {
	            
	            break;
	        }

	        // Create a Wraithling
	        Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
	        if (wraithling == null) {
	            
	            return;
	        }

	        // Set the owner of the Wraithling
	        wraithling.setOwner(gameState.getCurrentPlayer());

	        // Place the Wraithling on the vertical tile
	        gameState.getBoard().placeUnitOnTile(wraithling, verticalTile, false);

	        // Update the UI
	        BasicCommands.drawUnit(out, wraithling, verticalTile);
	        BasicCommands.setUnitHealth(out, wraithling, 1); // Wraithlings have 1 health
	        BasicCommands.setUnitAttack(out, wraithling, 1); // Wraithlings have 1 attack

	        // Add a delay between summoning each Wraithling for visual effect
	        try {
	            Thread.sleep(500); // 500ms delay
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	}
}