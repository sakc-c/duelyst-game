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
                //BasicCommands.drawTile(out, enemyTile, 2); // Highlight mode = 2 (Red)
                gameState.addRedHighlightedTile(enemyTile); // Track highlighted tiles
            }
        }
    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        // Check if the target tile is highlighted (valid target)
        if (gameState.getRedHighlightedTiles().contains(targetTile)) {

            // Destroy the enemy creature
            Unit enemyUnit = gameState.getBoard().getUnitOnTile(targetTile);
            if (enemyUnit == null) {

                return;
            }


            gameState.getBoard().removeUnitFromTile(targetTile, out);

            // Summon a Wraithling on the same tile
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            if (wraithling == null) {

                return;
            }
            wraithling.setOwner(gameState.getCurrentPlayer());

            // Place the Wraithling on the tile

            gameState.getBoard().placeUnitOnTile(gameState, wraithling, targetTile, false);

            // Add a small delay to ensure the unit is drawn
            try {
                Thread.sleep(500); // 100ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update the UI
            BasicCommands.drawUnit(out, wraithling, targetTile);
            try {
                Thread.sleep(100); // 100ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BasicCommands.setUnitHealth(out, wraithling, 1); // Wraithlings have 1 health
            BasicCommands.setUnitAttack(out, wraithling, 1); // Wraithlings have 1 attack
            wraithling.setCurrentHealth(1);
            wraithling.setAttackPower(1);

            // Play the idle animation
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
//    	// Check if the target tile is highlighted (valid target)
//        	if (gameState.isHighlightedTile(targetTile))  {
//                System.out.println("Applying Dark Terminus effect on tile (" + targetTile.getTilex() + "," + targetTile.getTiley() + ")");
//            // Destroy the enemy creature
//                gameState.getBoard().removeUnitFromTile(targetTile, out);
//
//
//            // Summon a Wraithling on the same tile
//                Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
//                if (wraithling == null) {
//                    System.out.println("Error: Failed to load Wraithling unit.");
//                    return;
//                }
//                wraithling.setOwner(gameState.getCurrentPlayer());
//                try {
//                    Thread.sleep(500); // 500ms delay
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
////            // Update the UI
//                BasicCommands.drawUnit(out, wraithling, targetTile);
//                BasicCommands.setUnitHealth(out, wraithling, 1); // Wraithlings have 1 health
//                BasicCommands.setUnitAttack(out, wraithling, 1); // Wraithlings have 1 attack
//                BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);
//
//            // Add a small delay for visual effect
//            try {
//                Thread.sleep(500); // 500ms delay
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

