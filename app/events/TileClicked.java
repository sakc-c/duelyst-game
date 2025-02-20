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
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
		Unit unitOnTile = gameState.getBoard().getUnitOnTile(tilex,tiley);

		if (gameState.getSourceTile() != null &&
				gameState.getSourceTile().equals(clickedTile)) {
			// Clear highlights if the same tile is clicked again
			clearHighlights(gameState, out);
			gameState.setSourceTile(null); // Reset the source tile
		} else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
			// Highlight valid tiles and set the source tile
			highlightValidTiles(tilex, tiley, gameState, out);
			gameState.setSourceTile(clickedTile); // Set the source tile
		}
	}

	private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
		// Clear previous highlights
		clearHighlights(gameState, out);

		// Define movement ranges
		int[][] validDirections = {{-2, 0}, {-1, 0}, {1, 0},{2, 0}, {0, -2}, {0, 2},{0, -1}, {0,1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // 2 tiles in cardinal directions

		// Highlight directions
		for (int[] direction : validDirections) {
			int newX = tileX + direction[0];
			int newY = tileY + direction[1];
			if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5 &&
					gameState.getBoard().getUnitOnTile(newX, newY) == null) {
				Tile tile = gameState.getBoard().getTile(newX,newY);
				BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
				gameState.addHighlightedTile(tile); // Track highlighted tiles
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
		gameState.clearHighlightedTiles(); // Clear the list
	}


	}
