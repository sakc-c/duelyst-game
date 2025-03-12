package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.*;
import structures.basic.*;

import static java.lang.Thread.sleep;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * <p>
 * {
 * messageType = “tileClicked”
 * tilex = <x index of the tile>
 * tiley = <y index of the tile>
 * }
 *
 * @author Dr. Richard McCreadie
 */

// might need to make some helper methods given all these if conditions, will work on refactoring the code.
// Was trying to build logic and it being functional first.
public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        //current clicks
        Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
        Unit unitOnTile = gameState.getBoard().getUnitOnTile(clickedTile);

        //any stored data
        Card selectedCard = gameState.getSelectedCard();
        Tile sourceTile = gameState.getSourceTile();
        Unit selectedUnit = gameState.getSelectedUnit();

        if (selectedCard != null) {
            if (selectedCard.isCreature()) {
                gameState.handleCreatureCardClick(out, clickedTile, selectedCard);
                gameState.setSelectedCard(null);
            } else {
                gameState.handleSpellCardClick(out, clickedTile);
                gameState.setSelectedCard(null);
            }
        } else if (sourceTile != null) {
            if (sourceTile.equals(clickedTile)) { //if same as clicked tile, clear highlights
                gameState.clearAllHighlights(out);
                gameState.setSourceTile(null);
                gameState.setSelectedUnit(null);
            } else if (selectedUnit != null) { //if selected unit exists
                //selected for attack
                if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer() && !selectedUnit.hasAttacked()) {
                    gameState.handleAttack(out, unitOnTile);
                    gameState.setSourceTile(null);
                    gameState.setSelectedUnit(null);
                }
                // Selected for movement
                else if (gameState.isHighlightedTile(clickedTile) && !selectedUnit.hasMoved()) {
                    gameState.handleMovement(out, clickedTile, selectedUnit);
                    gameState.clearAllHighlights(out);
                }
                // Clicked on a non-highlighted tile(not valid to move/attack), reset selection
                else {
                    gameState.clearAllHighlights(out);
                    gameState.setSourceTile(null);
                    gameState.setSelectedUnit(null);
                }
            }
        }
        // If no card or unit is selected before, highlight valid tiles for the current player's unit
        else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
            if (!unitOnTile.hasMoved() && unitOnTile.canMove() && !unitOnTile.isStunned()) {
                gameState.getValidMovementTiles (tilex, tiley, out); //setting in lists in gameState
                highlightValidTiles(gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            } else if (!unitOnTile.hasAttacked() && !unitOnTile.isStunned()) {
                gameState.getValidAttackTiles(clickedTile); //setting in lists in gameState
                highlightValidAttackTiles(gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            }
        }
    }


    private void highlightValidTiles(GameState gameState, ActorRef out) {
        for (Tile tile: gameState.getHighlightedTiles()) {
            BasicCommands.drawTile(out, tile,1);
        }
        for(Tile tile: gameState.getRedHighlightedTiles()) {
            BasicCommands.drawTile(out,tile,2);
        }

    }

    private void highlightValidAttackTiles(GameState gameState, ActorRef out) {
        for(Tile tile: gameState.getRedHighlightedTiles()) {
            BasicCommands.drawTile(out,tile,2);
        }
    }
   
}
    
    



