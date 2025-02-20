package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.ArrayList;
import java.util.List;

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

		if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
			highlightValidTiles(tilex,tiley, gameState, out);
		}

		if (gameState.isHighlightedTile(clickedTile)) {
			// Clear previous highlights
			clearHighlights(gameState, out);
		}
	}

	private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
		// Clear previous highlights
		clearHighlights(gameState, out);

		// Define movement ranges
		int[][] cardinalDirections = {{-2, 0}, {-1, 0}, {1, 0},{2, 0}, {0, -2}, {0, 2},{0, -1}, {0,1}}; // 2 tiles in cardinal directions
		int[][] diagonalDirections = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // 1 tile diagonally
		List<Tile> validTiles = new ArrayList<>();

		// Highlight cardinal directions
		for (int[] direction : cardinalDirections) {
			int newX = tileX + direction[0];
			int newY = tileY + direction[1];
			if (isValidTile(newX, newY, gameState)) {
				validTiles.add(gameState.getBoard().getTile(newX, newY));
			}
		}

		// Highlight diagonal directions
		for (int[] direction : diagonalDirections) {
			int newX = tileX + direction[0];
			int newY = tileY + direction[1];
			if (isValidTile(newX, newY, gameState)) {
				validTiles.add(gameState.getBoard().getTile(newX, newY));
			}
		}

		// perform a single batch highlight
		for (Tile tile : validTiles) {
			BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
			gameState.addHighlightedTile(tile); // Track highlighted tiles
		}
	}

	private boolean isValidTile(int tileX, int tileY, GameState gameState) {
		// Check if tile is within bounds and not occupied
		return tileX >= 0 && tileX < 9 && tileY >= 0 && tileY < 5 &&
				gameState.getBoard().getUnitOnTile(tileX, tileY) == null;
	}

	private void clearHighlights(GameState gameState, ActorRef out) {
		for (Tile tile : gameState.getHighlightedTiles()) {
			BasicCommands.drawTile(out, tile, 0); // Reset highlight mode = 0
		}
		gameState.clearHighlightedTiles(); // Clear the list
	}


	}
