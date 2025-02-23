package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private Tile[][] tiles; // 9x5 grid of tiles
    private ActorRef out;
    private Map<Tile, Unit> unitMap; // Track which unit is on which tile

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


    public void placeUnitOnTile(Unit unit, Tile tile) {


        // Remove the unit from its current tile (if any)
       if (unitMap.containsValue(unit)) {
            Tile currentTile = getTileForUnit(unit);
            if (currentTile != null) {
                unitMap.remove(currentTile); //from the map remove the key-value pair to keep map updated
                BasicCommands.moveUnitToTile(out, unit, tile);
                BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.move);
                try {
                    Thread.sleep(200); // Small delay for animation
                } catch (InterruptedException e) {
                    System.out.println("Error");
                }
                unitMap.put(tile,unit);
                unit.setPositionByTile(tile);
                unit.setHasMoved(true);
                return;
            }
        }
        //if unit not on the board then
        unit.setPositionByTile(tile);
        unitMap.put(tile, unit);
        BasicCommands.drawUnit(out, unit, tile);
    }

    public Unit getUnitOnTile(Tile tile) {
        return unitMap.get(tile); // Returns the unit if one is on this tile, otherwise null
    }

    public void removeUnitFromTile(Tile tile) {
        Unit unit = unitMap.get(tile); // Returns the unit to remove
        BasicCommands.deleteUnit(out, unit);
        unitMap.remove(tile); // Remove from map
    }

    public Map<Tile, Unit> getUnitMap() {
        return unitMap;
    }

    public Tile getTile(int x, int y) {
        return tiles[x][y]; // Retrieve a specific tile
    }
    
    
    

}
