package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
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

	




	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		int tilex = message.get("tilex").asInt();
	    int tiley = message.get("tiley").asInt();

	    Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
	    Unit unitOnTile = gameState.getBoard().getUnitOnTile(clickedTile);
	    Card selectedCard = gameState.getSelectedCard();

	    // Check if a card is selected
	    if (selectedCard != null) {
	        if (selectedCard.isCreature()) {
	            handleCreatureCardClick(out, gameState, clickedTile, unitOnTile, selectedCard);
	        } else {
	            handleSpellCardClick(out, gameState, clickedTile, unitOnTile);
	        }
	    }

	    // Check if source tile is clicked again - remove highlight
	    else if (gameState.getSourceTile() != null && gameState.getSourceTile().equals(clickedTile)) {
	        gameState.clearAllHighlights(out);
	        gameState.setSourceTile(null);
	        gameState.setSelectedUnit(null);
	    }

	    // If tile is selected for movement or attack
	    else if (gameState.getSelectedUnit() != null) {
	        // Selected for attack
	        if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer()) {
	            handleAttack(out, gameState, gameState.getSelectedUnit(), unitOnTile);
	        }

	        // Selected for movement
	        else if (gameState.isHighlightedTile(clickedTile) && !gameState.getSelectedUnit().hasMoved()) {
	            gameState.getBoard().placeUnitOnTile(gameState.getSelectedUnit(), clickedTile);
	            gameState.clearAllHighlights(out);
	            gameState.setSourceTile(null);
	            gameState.setSelectedUnit(null);
	        }

	        // Clicked on a non-highlighted tile, reset selection
	        else {
	            gameState.clearAllHighlights(out);
	            gameState.setSourceTile(null);
	            gameState.setSelectedUnit(null);
	        }
	    }

	    // If no card or unit is selected, highlight valid tiles for the current player's unit
	    else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
	        if (!unitOnTile.hasMoved()) {
	            gameState.clearAllHighlights(out);
	            highlightValidTiles(tilex, tiley, gameState, out);
	            gameState.setSourceTile(clickedTile);
	            gameState.setSelectedUnit(unitOnTile);
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
	
	private boolean isAdjacentTile(Tile tile1, Tile tile2) {
	    int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
	    int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
	    return dx <= 1 && dy <= 1; // Adjacent if within 1 tile in any direction
	}
	
	private Tile findEmptyAdjacentTile(GameState gameState, Tile targetTile) {
	    int[][] directions = {
	        {-1, 0}, {1, 0}, // Left, Right
	        {0, -1}, {0, 1}, // Up, Down
	        {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
	    };

	    for (int[] dir : directions) {
	        int newX = targetTile.getTilex() + dir[0];
	        int newY = targetTile.getTiley() + dir[1];

	        // Check if the new coordinates are within the board bounds
	        if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
	            Tile potentialTile = gameState.getBoard().getTile(newX, newY);
	            if (gameState.isHighlightedTile(potentialTile)) {
	            	return potentialTile;
	            	
	            }
	        }
	    }
	    return null; // No empty adjacent tile found
	}
	
	private void handleAttack(ActorRef out, GameState gameState, Unit attacker, Unit target) {
	    // Check if the attacker and target are valid
	    if (attacker == null || target == null) {
	        
	        return;
	    }

	    // Get the tiles for the attacker and target
	    Tile attackerTile = gameState.getBoard().getTileForUnit(attacker);
	    Tile targetTile = gameState.getBoard().getTileForUnit(target);

	    if (attackerTile == null || targetTile == null) {
	       
	        return;
	    }

	    // Check if the attacker is adjacent to the target
	    if (!isAdjacentTile(attackerTile, targetTile)) {
	        // Move to an adjacent tile
	        Tile adjacentTile = findEmptyAdjacentTile(gameState, targetTile);
	        if (adjacentTile == null) {
	            
	            return;
	        }

	        // Move the attacker to the adjacent tile
	        gameState.getBoard().placeUnitOnTile(attacker, adjacentTile);
	        //BasicCommands.drawUnit(out, attacker, adjacentTile); // Update UI

	        // Wait for movement to complete (optional)
	        try {
	            Thread.sleep(600); // Simulate movement delay
	        } catch (InterruptedException e) {
	            
	        }
	    }

	    // Perform the attack
	    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
	    target.takeDamage(attacker.getAttackPower());
	    BasicCommands.setUnitHealth(out, target, target.getCurrentHealth());

		/*
		 * // Check if the target is defeated if (target.getCurrentHealth() <= 0) {
		 * gameState.getBoard().removeUnit(target); // Remove from the board
		 * BasicCommands.deleteUnit(out, target); // Remove from the UI
		 * 
		 * }
		 */

	    // Mark the attacker as having moved
	    attacker.setHasMoved(true);
	    gameState.clearAllHighlights(out); // Clear highlights after the attack
	}


	/*
	 * public boolean handleAttacks(GameState gameState, ActorRef out, Unit
	 * targetUnit){ if (unitOnTile != null && unitOnTile.getOwner() ==
	 * gameState.getOpponentPlayer()) { Unit attackingUnit =
	 * gameState.getSelectedUnit(); //get the attacking unit if (attackingUnit !=
	 * null) { int damage = attackingUnit.getAttackPower();
	 * 
	 * unitOnTile.takeDamage(damage); //apply damage
	 * 
	 * if (unitOnTile != null) { BasicCommands.setUnitHealth(out, unitOnTile,
	 * unitOnTile.getCurrentHealth()); } }
	 * 
	 * } }
	 */
	
	


}
