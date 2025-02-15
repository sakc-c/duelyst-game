package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class Board {
    private Tile[][] tiles; // 9x5 grid of tiles
    private Unit playerAvatar; // The player's Avatar unit
    private Unit aiAvatar;
    private ActorRef out;

    public Board(ActorRef out) {
        this.out = out;
        tiles = new Tile[9][5]; // Initialize the board
        initializeTiles(); // Generate tiles
        placePlayerAvatar(); // Place the player's Avatar at (2,3)
        placeAIAvatar(); // place AI avatar at mirrored position of human player
    }

    private void initializeTiles() {
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y] = BasicObjectBuilders.loadTile(x, y);
                BasicCommands.drawTile(out, tiles[x][y], 0);
            }
        }
    }

    private void placePlayerAvatar() {
        Tile avatarTile = tiles[2][3]; // Get the tile at (2,3)
        playerAvatar = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 1, Unit.class);// loading the player avatar
        Position avatarPosition = new Position(0, 0, 2, 3); // placing the player avatar
        playerAvatar.setPosition(avatarPosition);
        BasicCommands.drawUnit(out, playerAvatar, avatarTile); // Draw the player Avatar
    }

    private void placeAIAvatar() {
        int mirroredx = 9-2-1; // calculating the mirrored position
        Tile aiTile =  tiles[mirroredx][3];
        aiAvatar = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 2, Unit.class); // loading AI avatar
        Position aiPosition = new Position(0, 0, mirroredx, 3);//placing the AI avatar
        aiAvatar.setPosition(aiPosition); // Assigns the AI avatar's position, including tile coordinates (tilex, tiley)
// and pixel coordinates (xpos, ypos), so it can be drawn at the correct location on the board
        BasicCommands.drawUnit(out, aiAvatar, aiTile); // Draw the AI Avatar

    }

    public Tile getTile(int x, int y) {
        return tiles[x][y]; // Retrieve a specific tile
    }

    public Unit getPlayerAvatar() {
        return playerAvatar; // Return the stored Avatar unit
    }
    public Unit getAIAvatar() {
        return aiAvatar;//return the stored AI avatar unit
    }
}
