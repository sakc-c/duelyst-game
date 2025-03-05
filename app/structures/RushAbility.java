package structures;

import akka.actor.ActorRef;
import structures.basic.Tile;
import structures.basic.Unit;

public class RushAbility implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        // Get the unit on the tile
        Unit unit = tile.getUnit();

        if (unit != null) {
            // Allow the unit to act immediately (move and attack)
            unit.setHasMoved(false); // Reset move state
            unit.setHasAttacked(false); // Reset attack state

        }
    }
}

// SummonCreature
// // Trigger Rush Ability
//    if (newUnit.getAbility() instanceof RushAbility) {
//        newUnit.getAbility().triggerAbility(out, gameState, tile);
//    }