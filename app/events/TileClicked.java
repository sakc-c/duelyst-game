package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import structures.HumanPlayer;

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

	//helper method to handle attack
	public void handleAttack(ActorRef out, GameState gameState, Unit attacker, Unit opponent) {
		BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);

		// Apply damage

		opponent.takeDamage(attacker.getAttackPower());
		BasicCommands.setUnitHealth(out, opponent, opponent.getCurrentHealth());
		gameState.clearAllHighlights(out);
		/* to remove the opponent unit if helath==0
		if (opponent.getCurrentHealth() == 0) {
			Tile opponentTile = gameState.getBoard().getTileForUnit(opponent);
			gameState.getBoard().removeUnitFromTile(opponentTile);
			BasicCommands.deleteUnit(out, opponent);
		}*/
		attacker.setHasMoved(true);
	}





	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
		Unit unitOnTile = gameState.getBoard().getUnitOnTile(clickedTile);
		Card selectedCard = gameState.getSelectedCard();


		// Check if a card is selected
		if (selectedCard != null) {
			// If the selected card is a creature card
			if (selectedCard.isCreature()) {
				handleCreatureCardClick(out, gameState, clickedTile, unitOnTile, selectedCard);
			}
			// If the selected card is a spell card
			else {
				handleSpellCardClick(out, gameState, clickedTile, unitOnTile);
			}
		}
		
		// check if source tile is clicked again - remove highlight
		else if (gameState.getSourceTile() != null && gameState.getSourceTile().equals(clickedTile)) {
			gameState.clearAllHighlights(out);
			gameState.setSourceTile(null); // Reset the source tile
			gameState.setSelectedUnit(null); //Reset the selected unit
		}

		//if tile is selected for movement or attack
		else if (gameState.getSelectedUnit() != null) {
			//selected for attack
			if(unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer()){
					Tile opponentTile = gameState.getBoard().getTileForUnit(unitOnTile);

					//if source tile is already adjacent, attack directly
					if (isAdjacentTile(gameState.getSourceTile(), opponentTile)) {
						handleAttack(out, gameState, gameState.getSelectedUnit(), unitOnTile);
						return;
					}

					//Find closet empty adjacent tile
					Tile adjacentTile = null;
					int[][] directions = {
							{-1, 0}, {1, 0}, // Left, Right
							{0, -1}, {0, 1}, // Up, Down
							{-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
					};

					// Loop through adjacent tiles
					for (int[] dir : directions) {
						int newX = opponentTile.getTilex() + dir[0];
						int newY = opponentTile.getTiley() + dir[1];

						// Check if tile is within board limits
						if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
							Tile potentialTile = gameState.getBoard().getTile(newX, newY);

							if (gameState.isHighlightedTile(potentialTile)) {
								adjacentTile = gameState.getBoard().getTile(newX, newY);
								break;
							}
						}
					}
				gameState.getBoard().placeUnitOnTile(gameState.getSelectedUnit(), adjacentTile);
				try{Thread.sleep(600);}
				catch (InterruptedException e){
								System.out.println("Error in movement delay");
							}
				handleAttack(out, gameState, gameState.getSelectedUnit(), unitOnTile);

//					// Move unit to adjacent tile before attacking
//					if (adjacentTile != null) {
//						gameState.getBoard().placeUnitOnTile(gameState.getSelectedUnit(), adjacentTile);
//
//						// Wait for movement to complete before attacking
//							try{
//								Thread.sleep(600);
//							} catch (InterruptedException e){
//								System.out.println("Error in movement delay");
//							}
//							//Attack after movement
//						handleAttack(out, gameState, gameState.getSelectedUnit(), unitOnTile);
//
//
//					}

			}

			if (gameState.isHighlightedTile(clickedTile) && !gameState.getSelectedUnit().hasMoved()) {
//				Update the unit's position using setPositionByTile
//			    gameState.getSelectedUnit().setPositionByTile(clickedTile);
				gameState.getBoard().placeUnitOnTile(gameState.getSelectedUnit(), clickedTile);
				gameState.clearAllHighlights(out);
				gameState.setSourceTile(null);
				gameState.setSelectedUnit(null);
			}
			//clicked on a non-highlighted tile, reset selection & no movement (should we do any notification?)
			else {
				gameState.clearAllHighlights(out);
				gameState.setSourceTile(null);
				gameState.setSelectedUnit(null);
			}
		}
		//if both conditions above are false - source tile & selected unit are null. Action: valid tiles to be highlighted
		else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
			if (!unitOnTile.hasMoved()) {
				gameState.clearAllHighlights(out);
				highlightValidTiles(tilex, tiley, gameState, out);
				gameState.setSourceTile(clickedTile); // Set the source tile
				gameState.setSelectedUnit(unitOnTile); // Set the selected unit
			}
		}
        
	}
	
	private void handleCreatureCardClick(ActorRef out, GameState gameState, Tile clickedTile, Unit unitOnTile, Card selectedCard) {
        // Check if the clicked tile is valid for summoning
        if (gameState.isHighlightedTile(clickedTile)) {
			// Summon the creature on the clicked tile
			summonCreature(out, gameState, clickedTile);

			// Get the current player
			HumanPlayer currentPlayer = (HumanPlayer) gameState.getCurrentPlayer();

			// Play the card (remove from hand and deduct mana)
			currentPlayer.playCard(selectedCard, out);

            gameState.clearAllHighlights(out); // Clear highlights after summoning
            gameState.setSelectedCard(null); // Reset the selected card
        } else {
            // Clicked on an invalid tile, reset selection
            gameState.clearAllHighlights(out);
            gameState.setSelectedCard(null);
        }
    }
	
	private void handleSpellCardClick(ActorRef out, GameState gameState, Tile clickedTile, Unit unitOnTile) {
        // Handle spell-specific logic (e.g., cast spell on the clicked tile)
        // For now, just clear highlights and reset the selected card
        gameState.clearAllHighlights(out);
        gameState.setSelectedCard(null);
    }
	
	private void summonCreature(ActorRef out, GameState gameState, Tile tile) {
        // Implement logic to summon the creature on the tile
        Card selectedCard = gameState.getSelectedCard();
        Unit newUnit = BasicObjectBuilders.loadUnit(selectedCard.getUnitConfig(), gameState.getNextUnitId(), Unit.class);
        newUnit.setOwner(gameState.getCurrentPlayer());
        gameState.getBoard().placeUnitOnTile(newUnit, tile);
		newUnit.setHasMoved(true);
        //BasicCommands.drawUnit(out, newUnit, tile); //placeUnitOnTile already has draw, this was re-drawing
    }

	private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {

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

	
	
	


}
