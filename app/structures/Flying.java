package structures;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import commands.BasicCommands;

public class Flying implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(tile); // Use the provided tile parameter
        if (sourceUnit == null) {
            return; // Exit if there's no unit on the tile
        }

        highlightValidTiles(tile.getTilex(), tile.getTiley(), gameState, out);

        }
    }

    private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
        gameState.clearAllHighlights(out);

        Unit selectedUnit = gameState.getSelectedUnit();

        if (selectedUnit != null && selectedUnit.getAbility() instanceof Flying) {
            // Flying units can move to any unoccupied tile
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 5; y++) {
                    Tile tile = gameState.getBoard().getTile(x, y);
                    if (gameState.getBoard().getUnitOnTile(tile) == null) {
                        BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
                        gameState.addHighlightedTile(tile);
                    }
                }
            }
        }

}