package structures;

import akka.actor.ActorRef;
import structures.basic.Tile;
import structures.basic.Unit;

public class RushAbility implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile, Tile targetTile) {
        // Implementation for when source and target tiles are provided
        // (Not used for RushAbility, but required by the interface)
    }

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        // Get the unit on the tile
        Unit unit = tile.getUnit();

        if (unit != null) {
            // Allow the unit to act immediately (move and attack)
            unit.setHasMoved(false); // Reset move state
            unit.setHasAttacked(false); // Reset attack state

            // Notify the board or game state that this unit can act
            gameState.allowUnitToAct(unit);

            System.out.println("RushAbility: Unit " + unit.getName() + " can now move and attack!");
        }
    }
}