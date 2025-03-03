package structures;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import commands.BasicCommands;

public class Flying implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                Tile newTile = gameState.getBoard().getTile(x, y);
                Unit newUnit = gameState.getBoard().getUnitOnTile(newTile);
                if (newUnit == null && newTile != tile ) {
                    BasicCommands.drawTile(out, newTile, 1); // Highlight mode = 1
                    gameState.addHighlightedTile(newTile);
                }
            }
        }

    }
}