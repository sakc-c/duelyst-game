package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HornOfTheForsaken implements SpellEffect, OnHitEventListener {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
    	 // Clear all existing highlights
        gameState.clearAllHighlights(out);

        // Get the player's avatar
        Unit avatar = gameState.getCurrentPlayer().getAvatar();
        Tile avatarTile = gameState.getBoard().getTileForUnit(avatar);

        // Highlight the avatar's tile
        if (avatarTile != null) {
            BasicCommands.drawTile(out, avatarTile, 1); // Highlight mode = 1 (Blue)
            gameState.addHighlightedTile(avatarTile); // Track highlighted tiles
        }
    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
    	// Get the unit on the targetTile to applyEffect to
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);

        // Equip the artifact to the player's avatar
        Player currentPlayer = gameState.getCurrentPlayer();
        currentPlayer.equipArtifact(3); // Initialize with 3 robustness

        // Update the UI to indicate the artifact is equipped
        BasicCommands.addPlayer1Notification(out, "Horn of the Forsaken equipped!", 2);

     // Add the "On Hit" effect to the player's avatar
        unit.addOnHitEventListener(this);
    }

    @Override
    public void onHit(ActorRef out, GameState gameState) {
    	// Get the player's avatar
        Unit avatar = gameState.getCurrentPlayer().getAvatar();
        Tile avatarTile = gameState.getBoard().getTileForUnit(avatar);

        // Get adjacent tiles
        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState, avatarTile);

        // Filter unoccupied tiles
        List<Tile> unoccupiedTiles = new ArrayList<>();
        for (Tile tile : adjacentTiles) {
            if (gameState.getBoard().getUnitOnTile(tile) == null) {
            	// Add a small delay for visual effect
                try {
                    Thread.sleep(200); // 500ms delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            	unoccupiedTiles.add(tile);
            }
        }

        // If there are unoccupied tiles, summon a Wraithling on a random one
        if (!unoccupiedTiles.isEmpty()) {
            Random random = new Random();
            Tile randomTile = unoccupiedTiles.get(random.nextInt(unoccupiedTiles.size()));

            // Summon a Wraithling
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            wraithling.setOwner(gameState.getCurrentPlayer());
            wraithling.setCurrentHealth(1); // Wraithlings have 1 health
            wraithling.setAttackPower(1);   // Wraithlings have 1 attack

            // Place the Wraithling on the tile
            gameState.getBoard().placeUnitOnTile(wraithling, randomTile, false);
            
         // Add a small delay for visual effect
            try {
                Thread.sleep(200); // 500ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update the UI
            BasicCommands.setUnitHealth(out, wraithling, wraithling.getCurrentHealth());
            BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackPower());
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
