package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * 
 * { 
 *   messageType = “tileClicked”
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */

// might need to make some helper methods given all these if conditions, will work on refactoring the code.
// Was trying to build logic and it being functional first.
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
		Unit unitOnTile = gameState.getBoard().getUnitOnTile(clickedTile);


		// check if source tile is clicked again - remove highlight
		if (gameState.getSourceTile() != null &&
				gameState.getSourceTile().equals(clickedTile)) {
			clearHighlights(gameState, out);
			gameState.setSourceTile(null); // Reset the source tile
			gameState.setSelectedUnit(null); //Reset the selected unit
		} //if tile is selected for movement or attack
		else if (gameState.getSelectedUnit() != null) {
			//selected for attack
			//if(unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer()){
				// @Alaa: Implement the logic for handling an attack. When the player selects an opponent's unit (red tile),
				// the unit should not move to the opponent's tile. Instead, it should move to the adjacent tile and then perform an attack.
			//}

			if (gameState.isHighlightedTile(clickedTile) && !gameState.getSelectedUnit().hasMoved()) {
				gameState.getBoard().placeUnitOnTile(gameState.getSelectedUnit(), clickedTile);
				clearHighlights(gameState,out);
				gameState.setSourceTile(null);
				gameState.setSelectedUnit(null);
			} else {
				//clicked on a non-highlighted tile, reset selection & no movement (should we do any notification?)
				clearHighlights(gameState, out);
				gameState.setSourceTile(null);
				gameState.setSelectedUnit(null);
			}
		}
		//if both conditions above are false - source tile & selected unit are null. Action: valid tiles to be highlighted
		else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
			if (!unitOnTile.hasMoved()) {
				highlightValidTiles(tilex, tiley, gameState, out);
				gameState.setSourceTile(clickedTile); // Set the source tile
				gameState.setSelectedUnit(unitOnTile); // Set the selected unit
			}
		}
	}

	private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
		// Clear previous highlights
		clearHighlights(gameState, out);

		// Define movement ranges
		int[][] validDirections = {{-2, 0}, {-1, 0}, {1, 0},{2, 0}, {0, -2}, {0, 2},{0, -1}, {0,1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

		// Highlight directions
		for (int[] direction : validDirections) {
			int newX = tileX + direction[0];
			int newY = tileY + direction[1];

			// Check if the new coordinates are within the board bounds
			if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
				Tile tile = gameState.getBoard().getTile(newX, newY);
				Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

				// Highlight empty tiles
				if (unitOnTile == null) {
					BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
					gameState.addHighlightedTile(tile); // Track highlighted tiles
				}
				// Highlight tiles with opponent's units
				else if (unitOnTile.getOwner() == gameState.getOpponentPlayer()) {
					BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2
					gameState.addHighlightedTile(tile); // Track highlighted tiles
				}
			}
		}

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Error");
		}

	}

	private void clearHighlights(GameState gameState, ActorRef out) {
		for (Tile tile : gameState.getHighlightedTiles()) {
			BasicCommands.drawTile(out, tile, 0); // Reset highlight mode = 0
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Error");
		}
		gameState.clearHighlightedTiles(); // Clear the list in gameState
	}


	}
