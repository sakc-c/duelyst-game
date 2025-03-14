package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.List;

/**
 * Implementation of the Provoke ability.
 * Enemy units in adjacent squares cannot move and can only attack this unit or other units with Provoke.
 * When activated, adjacent enemy units have their movement restricted and can only target units with Provoke.
 */
public class Provoke implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);

        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(sourceTile);

        List<Unit> provokeUnits = gameState.getBoard().getUnitsWithAbility(Provoke.class);

        for (Tile adjacentTile : adjacentTiles) {
            Unit enemyUnit = gameState.getBoard().getUnitOnTile(adjacentTile);

            if (enemyUnit != null && enemyUnit.getOwner() != sourceUnit.getOwner() && !gameState.isUnitProvoked(enemyUnit)) {
                BasicCommands.addPlayer1Notification(out, "Provoke Active", 3);
                enemyUnit.setCanMove(false);
                // Restrict the enemy unit's attacks to the source unit or other units with Provoke
                enemyUnit.setValidAttackTargets(provokeUnits);
                gameState.addProvokeEffect(enemyUnit, sourceUnit);

            }
        }


    }
}
