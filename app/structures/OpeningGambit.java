package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.List;

public class OpeningGambit implements Ability {
    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);
        String unitName = sourceUnit.getName();

        switch (unitName) {
            case "Gloom Chaser":
                triggerGloomChaserAbility(out, gameState, sourceTile);
                break;
            case "Nightsorrow Assassin":
                triggerNightsorrowAssassinAbility(out, gameState, sourceTile);
                break;
            case "Silverguard Squire":
                triggerSilverguardAbility(out, gameState, sourceTile);
                break;
        }
    }

    private void triggerSilverguardAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        BasicCommands.addPlayer1Notification(out, "Opening Gambit triggered!", 2);

        Unit SilverguardSquire = gameState.getBoard().getUnitOnTile(sourceTile);
        if (SilverguardSquire == null) {
            System.out.println("Error: Silverguard Squire not found on source tile.");
            return;
        }

        // Get the player's avatar and tile
        Unit avatar = SilverguardSquire.getOwner().getAvatar();
        Tile avatarTile = gameState.getBoard().getTileForUnit(avatar);

        // Get the avatar's position
        int avatarX = avatarTile.getTilex();
        int avatarY = avatarTile.getTiley();

        // Define the positions directly in front (left) and behind (right) the avatar
        int[][] directions = {
                {-1, 0}, // Left (directly in front)
                {1, 0}   // Right (directly behind)
        };

        // Check the tiles directly in front and behind the avatar
        for (int[] direction : directions) {
            int newX = avatarX + direction[0];
            int newY = avatarY + direction[1];

            // Check if the new coordinates are within the board bounds
            if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                Tile adjacentTile = gameState.getBoard().getTile(newX, newY);
                Unit adjacentUnit = gameState.getBoard().getUnitOnTile(adjacentTile);

                // Check if the adjacent unit is an allied unit
                if (adjacentUnit != null && adjacentUnit.getOwner() == SilverguardSquire.getOwner()) {
                    // Increase the unit's attack by 1
                    int newAttack = adjacentUnit.getAttackPower() + 1;
                    adjacentUnit.setAttackPower(newAttack);

                    // Increase the unit's health by 1 (both current and maximum health)
                    int newHealth = adjacentUnit.getCurrentHealth() + 1;
                    adjacentUnit.setCurrentHealth(newHealth);
                    adjacentUnit.setMaximumHealth(adjacentUnit.getMaxHealth() + 1);

                    // Update the unit's stats in the UI
                    BasicCommands.setUnitAttack(out, adjacentUnit, newAttack);
                    BasicCommands.setUnitHealth(out, adjacentUnit, newHealth);
                    BasicCommands.playUnitAnimation(out, adjacentUnit, UnitAnimationType.idle);

                    // Add a small delay for UI updates
                    try {
                        Thread.sleep(200); // 200ms delay
                    } catch (InterruptedException e) {
                        System.out.println("Error during UI update delay");
                    }
                }
            }
        }
    }
    private void triggerGloomChaserAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        // Get the tile directly behind the Gloom Chaser (to its left for the human player)
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);
        boolean isHumanPlayer = sourceUnit.getOwner() instanceof HumanPlayer;
        Tile behindTile = gameState.getBoard().getTileBehind(sourceTile, isHumanPlayer);

        // Check if the tile is empty
        if (behindTile != null && gameState.getBoard().getUnitOnTile(behindTile) == null) {
            // Summon a Wraithling on the behindTile
            BasicCommands.addPlayer1Notification(out, "Opening Gambit Triggered", 3);
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            wraithling.setOwner(sourceUnit.getOwner());
            wraithling.setCurrentHealth(1); // Wraithlings have 1 health
            wraithling.setAttackPower(1);   // Wraithlings have 1 attack
            gameState.getBoard().placeUnitOnTile(gameState,wraithling, behindTile, false);

            // Add a small delay for place Unit to complete
            try {
                Thread.sleep(200); // 200ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update the UI
            BasicCommands.setUnitHealth(out, wraithling, wraithling.getCurrentHealth());
            BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackPower());
            BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);
        }
    }

    private void triggerNightsorrowAssassinAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        // Get all adjacent tiles
        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(sourceTile);

        // Find an enemy unit below max health
        for (Tile adjacentTile : adjacentTiles) {
            Unit enemyUnit = gameState.getBoard().getUnitOnTile(adjacentTile);
            if (enemyUnit != null && enemyUnit.getOwner() == gameState.getOpponentPlayer() && enemyUnit.getCurrentHealth() < enemyUnit.getMaxHealth()) {
                // Destroy the enemy unit
                BasicCommands.addPlayer1Notification(out, "Opening Gambit Triggered", 3);
                gameState.getBoard().removeUnitFromTile(adjacentTile, out);
                break; // Only destroy one unit
            }
        }
    }
}
