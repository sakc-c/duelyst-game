package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.HashMap;
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

    public void placePlayerAvatar(Unit playerAvatar) {
        Tile avatarTile = tiles[1][2];
        playerAvatar.setPositionByTile(avatarTile);
        BasicCommands.drawUnit(out, playerAvatar, avatarTile);
    }

    public void placeAIAvatar(Unit aiAvatar) {
        int mirroredX = 9 - 1 - 1;
        Tile aiTile = tiles[mirroredX][2];
        aiAvatar.setPositionByTile(aiTile);
        BasicCommands.drawUnit(out, aiAvatar, aiTile);
    }

    public Unit getUnitOnTile(Tile tile) {
        return unitMap.get(tile); // Returns the unit if one is on this tile, otherwise null
    }

    public void removeUnitFromTile(Tile tile) {
        Unit unit = unitMap.get(tile); // Returns the unit to remove
        BasicCommands.deleteUnit(out, unit);
        unitMap.remove(tile); // Remove from map
    }

}
