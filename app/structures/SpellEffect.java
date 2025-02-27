package structures;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;

public interface SpellEffect {
    void highlightValidTargets(ActorRef out, GameState gameState, Tile tile);
    void applyEffect(ActorRef out, GameState gameState, Tile targetTile);
}
