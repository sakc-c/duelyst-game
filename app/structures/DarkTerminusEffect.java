package structures;

import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import structures.basic.Tile;
import utils.StaticConfFiles;

public class DarkTerminusEffect implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        // Clear all existing highlights
        gameState.clearAllHighlights(out);

        // Get tiles occupied by enemy units
        List<Tile> enemyTiles = gameState.getTilesOccupiedByEnemyPlayer();

        // Highlight tiles with enemy creatures (excluding avatar)
        for (Tile enemyTile : enemyTiles) {
            Unit unitOnTile = gameState.getBoard().getUnitOnTile(enemyTile);
            if (unitOnTile != null && !unitOnTile.isAvatar()) {
                BasicCommands.drawTile(out, enemyTile, 2); // Highlight mode = 2 (Red)
                gameState.addHighlightedTile(enemyTile); // Track highlighted tiles
            }
        }
    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
    	// Check if the target tile is highlighted (valid target)
        	if (gameState.isHighlightedTile(targetTile))  {
            // Destroy the enemy creature
            gameState.getBoard().removeUnitFromTile(targetTile, out);

            // Summon a Wraithling on the same tile
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            wraithling.setOwner(gameState.getCurrentPlayer());

            // Place the Wraithling on the tile
            gameState.getBoard().placeUnitOnTile(gameState,wraithling, targetTile, false);
            
            try {
                Thread.sleep(500); // 500ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            // Update the UI
//            BasicCommands.drawUnit(out, wraithling, targetTile);
            BasicCommands.setUnitHealth(out, wraithling, 1); // Wraithlings have 1 health
            BasicCommands.setUnitAttack(out, wraithling, 1); // Wraithlings have 1 attack
            BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);

            // Add a small delay for visual effect
            try {
                Thread.sleep(500); // 500ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    } 
}
