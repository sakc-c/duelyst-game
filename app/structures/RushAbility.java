package structures;

import akka.actor.ActorRef;
import structures.basic.Tile;
import structures.basic.Unit;


/**
 * Implementation of the Rush ability.
 * Allows a unit to move and attack on the turn it is summoned.
 * When a unit with Rush is placed, its movement and attack states are reset to allow immediate action.
 */
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