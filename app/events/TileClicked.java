package events;


import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
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
public class TileClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int tilex = message.get("tile x").asInt();
		int tiley = message.get("tile y").asInt();
		Tile clickedTile = BasicObjectBuilders.loadTile(tilex, tiley);  // Get the tile

		// Beginning of summoning logic for ID15; check for mana, if not enough can't proceed.
		Card selectedCard = gameState.getSelectedCard();

		if (selectedCard != null && selectedCard.isCreature()) { //ensure its creature card
			Player currentPlayer = gameState.getCurrentPlayer();
			int manaCost = selectedCard.getManacost();

			if (currentPlayer.getMana() >= manaCost) {
				if (isValidSummonTile(clickedTile, gameState)) {
					// Summon the unit
					summonUnit(out, clickedTile, selectedCard, gameState);

					//update GameState
					currentPlayer.setMana(currentPlayer.getMana() - manaCost);
					BasicCommands.setPlayer1Mana(out, currentPlayer);  // Update UI
					BasicCommands.deleteCard(out, 1); //removes card from hand, needs to be changed from hardcoded.

					gameState.setSelectedCard(null); // Deselect the card
				} else {
					BasicCommands.addPlayer1Notification(out, "Invalid summon tile", 2);
				}
			} else {
				BasicCommands.addPlayer1Notification(out, "Not enough mana!", 2);
			}
		} else {
			BasicCommands.addPlayer1Notification(out, "Selected card is not a creature card!", 2);
		}
		}
	private boolean isValidSummonTile(Tile clickedTile, GameState gameState) {
		if (clickedTile == null) {
			System.err.println("Clicked tile is null");
			return false;
		}
		//Check if tile is occupied
		if(isTileOccupied(clickedTile, gameState)){
			System.err.println("Tile is occupied");
			return false;
		}
		//Check if tile is adjacent to friendly unit
		if(!isAdjacentToFriendlyUnit(clickedTile, gameState)){
			System.err.println("Tile is not adjacent to friendly unit");
			return false;
		}
		return true;
	}
	private boolean isTileOccupied(Tile clickedTile, GameState gameState){
		// Leverage the board's method to check occupancy
		return gameState.getBoard().getUnitOnTile(clickedTile) != null;
	}

	// Helper method for checking if a tile is adjacent to a friendly unit
	private boolean isAdjacentToFriendlyUnit(Tile clickedTile, GameState gameState) {
		Board board = gameState.getBoard();
		Player currentPlayer = gameState.getCurrentPlayer();

		int tileX = clickedTile.getTilex();
		int tileY = clickedTile.getTiley();

		// Offsets for the 8 adjacent tiles (including diagonals)
		int[] dx = {-1, -1, -1,  0, 0, 1, 1, 1};
		int[] dy = {-1,  0,  1, -1, 1,-1, 0, 1};

		for (int i = 0; i < dx.length; i++) {
			Tile neighborTile = board.getTile(tileX + dx[i], tileY + dy[i]);
			if (neighborTile != null) {
				Unit unit = board.getUnitOnTile(neighborTile);
				if (unit != null && unit.getPlayerId() == currentPlayer.getPlayerId()) {
					return true;
				}
			}
		}
		return false;
	}

	private void summonUnit(ActorRef out, Tile clickedTile, Card selectedCard, GameState gameState) {
		// Load unit configuration
		Unit unit = BasicObjectBuilders.loadUnit(selectedCard.getUnitConfig(), 0, Unit.class);  // Load the unit
		if (unit == null) {
			System.err.println("Failed to load unit from config: " + selectedCard.getUnitConfig());
			return;
		}

		unit.setPositionByTile(clickedTile);
		BasicCommands.drawUnit(out, unit, clickedTile);

		//Play summon effect
		EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
		BasicCommands.playEffectAnimation(out, effect, clickedTile);
		try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}  // Delay for effect

		//Potentially update board state
		//gameState.getBoard().placeUnitOnTile(unit, clickedTile); //doesnt exist yet but will be implemented.

	}

	private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
		// Clear previous highlights
		clearHighlights(gameState, out);

		// Define movement ranges
		int[][] validDirections = {{-2, 0}, {-1, 0}, {1, 0}, {2, 0}, {0, -2}, {0, 2}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

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