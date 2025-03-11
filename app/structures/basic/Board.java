package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.Ability;
import structures.GameState;
import structures.RushAbility;
import utils.BasicObjectBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private Tile[][] tiles; // 9x5 grid of tiles
    private ActorRef out;
    private static Map<Tile, Unit> unitMap; // Track which unit is on which tile

    public Board(ActorRef out) {
        this.out = out;
        tiles = new Tile[9][5]; // Initialize the board
        initializeTiles(); // Generate tiles
        unitMap = new HashMap<>(); // Initialize the unit tracking map
    }

    private void initializeTiles() {
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y] = BasicObjectBuilders.loadTile(x, y);
                BasicCommands.drawTile(out, tiles[x][y], 0);
            }
        }
    }

    public Tile getTileForUnit(Unit unit) {
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            if (entry.getValue().equals(unit)) {
                return entry.getKey();
            }
        }
        return null;
    }


    public void placeUnitOnTile(GameState gameState, Unit unit, Tile tile, boolean yFirst) {

        // Remove the unit from its current tile (if any)
       if (unitMap.containsValue(unit)) {
            Tile currentTile = getTileForUnit(unit);
            if (currentTile != null) {
                unitMap.remove(currentTile); //from the map remove the key-value pair to keep map updated
                BasicCommands.moveUnitToTile(out, unit, tile, yFirst);
                BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.move);
                try {
                    Thread.sleep(200); // Small delay for animation
                } catch (InterruptedException e) {
                    System.out.println("Error");
                }
                unitMap.put(tile,unit);
                unit.setPositionByTile(tile);
                unit.setHasMoved(true);
                gameState.triggerProvoke(out);
                return;
            }
        }
        //if unit not on the board then
        unit.setPositionByTile(tile);
        unitMap.put(tile, unit);
        BasicCommands.drawUnit(out, unit, tile);
        try {
            Thread.sleep(100); // Small delay for animation
        } catch (InterruptedException e) {
            System.out.println("Error");
        }
        unit.setHasAttacked(true);
        unit.setHasMoved(true);
        gameState.triggerProvoke(out);

        if (unit.getAbility() instanceof RushAbility) {
            unit.setHasAttacked(false);
            unit.setHasMoved(false);
        }
    }

    public Unit getUnitOnTile(Tile tile) {
        return unitMap.get(tile); // Returns the unit if one is on this tile, otherwise null
    }

    public void removeUnitFromTile(Tile tile, ActorRef out) {
        Unit unit = unitMap.get(tile); // Returns the unit to remove
        BasicCommands.deleteUnit(out, unit);
        unitMap.remove(tile); // Remove from map
    }

    public static Map<Tile, Unit> getUnitMap() {
        return unitMap;
    }

    public Tile getTile(int x, int y) {
        return tiles[x][y]; // Retrieve a specific tile
    }

    public List<Tile> getAdjacentTiles(GameState gameState, Tile tile) {
        List<Tile> adjacentTiles = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, // Left, Right
                {0, -1}, {0, 1}, // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
        };

        for (int[] dir : directions) {
            int newX = tile.getTilex() + dir[0];
            int newY = tile.getTiley() + dir[1];

            // Check if the new coordinates are within the board bounds
            if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                adjacentTiles.add(getTile(newX, newY));
            }
        }

        return adjacentTiles;
    }


    public Tile getTileBehind(Tile tile, Boolean isHuman) {
        int x = tile.getTilex();
        int y = tile.getTiley();

        // For the human player, "behind" is to the left (x - 1)
        // For the AI player, "behind" is to the right (x + 1)
        int behindX = isHuman ? x - 1 : x + 1;

        // Ensure the tile is within the board bounds
        if (behindX >= 0 && behindX < 9) {
            return getTile(behindX, y);
        }
        return null; // Tile is out of bounds


    }

    public List<Unit> getUnitsWithAbility(Class<? extends Ability> abilityClass) {
        List<Unit> unitsWithAbility = new ArrayList<>();
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Ability ability = unit.getAbility();
            if (ability != null && abilityClass.isInstance(ability)) {
                unitsWithAbility.add(unit);
            }
        }
        return unitsWithAbility;
    }

    public boolean isAdjacentTile(Tile tile1, Tile tile2) {
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        return dx <= 1 && dy <= 1; // Adjacent if within 1 tile in any direction
    }
}
