package structures;

import akka.actor.ActorRef;
import events.EndTurnClicked;
import structures.basic.Card;
import structures.basic.Player;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import commands.BasicCommands; // Import the BasicCommands
import utils.StaticConfFiles;

public class AIController extends Player {
    private List<Card> deck;  // All available cards
    private int health; // AI's health
    private ActorRef out;

    public AIController(int health, int mana, ActorRef out) {
        super(health, mana);
        this.deck = OrderedCardLoader.getPlayer2Cards(1);
        this.health = health;
        this.out = out;
    }

    //Getter method to retrieve health of AI
    public int getHealth() {
        return health;
    }


    public void drawInitialHand() {
        for (int i = 0; i < 3; i++) {
            drawCard();
        }
    }

    // The AI draws a card from its deck and adds it to the hand. No need to show on UI
    public void drawCard() {
        if (getHand().size() < 6 && !deck.isEmpty()) { // Ensure there's space and deck is not empty
            Card newCard = deck.remove(0); // Draw the first card from the deck
            getHand().add(newCard);
        }

    }

    public void playCard(Card card, ActorRef out, GameState gameState) {
        // Step 1: Play a card (if possible)
        Card cardToPlay = selectCardToPlay(gameState);
        if (cardToPlay != null) {
            Tile summonTile = selectSummonTile(gameState);
            if (summonTile != null) {
                this.summonCard(out, cardToPlay, summonTile);
            }
        }

        // Step 2: Move units
        Unit unitToMove = decideWhichUnitToMove(gameState);
        if (unitToMove != null) {
            //Tile nextTile = logic here
            //TODO: Bhumika to implement
            gameState.handleMovement(nextTile,unitToMove);
        }

        // Step 3: Attack with units
        attackWithUnits(out, gameState);

        // Step 4: Trigger end turn event processor
    }

    private Unit decideWhichUnitToMove(GameState gameState) {
        //TODO: Bhumika to implement
        return null;
    }

    private void summonCard(ActorRef out, Card cardToPlay, Tile summonTile) {
        //TODO: Alaa to implement
    }

    private Tile selectSummonTile(GameState gameState) {
        return null;
        //TODO: Alaa to implement
    }

    private Card selectCardToPlay(GameState gameState) {
        return null;
        //TODO: sarah to implement
    }

    private void attackWithUnits(ActorRef out, GameState gameState) {
        for (Tile tile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(tile);
            if (unit == null || unit.hasAttacked()) continue; // Skip if no unit or already attacked

            gameState.getValidAttackTiles(tile); //this methods set attackable tiles in gameStates list
            List<Tile> attackableTiles = gameState.getRedHighlightedTiles(); // Get attackable tiles for this unit
            Unit target = selectBestTarget(unit, attackableTiles, gameState);

            if (target != null) {
                gameState.setSelectedUnit(unit);
                gameState.handleAttack(out, target); // Ensure attack method has the attacker and target
            }
        }
    }

    private Unit selectBestTarget(Unit attacker, List<Tile> attackableTiles, GameState gameState) {
        Unit bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        for (Tile attackTile : attackableTiles) {
            Unit possibleTarget = gameState.getBoard().getUnitOnTile(attackTile);
            if (possibleTarget == null || !attacker.canAttack(possibleTarget)) continue; // Skip empty or invalid targets

            int score = calculateTargetScore(attacker, possibleTarget);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = possibleTarget;
            }
        }
        return bestTarget;
    }

    private int calculateTargetScore(Unit attacker, Unit target) {
        int score = 0;

        // Prioritize attacking the opponent's avatar
        if (target.isAvatar()) {
            score += 1000;
        }

        // Prioritize low-health targets to eliminate them quickly
        score += (10 - target.getCurrentHealth()) * 10;

        // Prioritize high-value targets (e.g., strong attack power)
        score += target.getAttackPower() * 5;

        // Avoid suicidal attacks where AI unit would die
        if (attacker.getCurrentHealth() <= target.getAttackPower()) {
            score -= 1000;
        }

        return score;
    }


}
