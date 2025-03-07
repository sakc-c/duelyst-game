package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import events.EndTurnClicked;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.OrderedCardLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AIController extends Player {
    private final GameState GameState;
    private List<Card> deck;  // All available cards
    private int health; // AI's health
    private ActorRef out;

    public AIController(int health, int mana, ActorRef out) {
        super(health, mana);
        this.deck = OrderedCardLoader.getPlayer2Cards(1);
        this.health = health;
        this.out = out;
        this.GameState = new GameState();
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
        // Step 1: Play a card and summon if possible
        selectCardToPlay(gameState);

        // Step 2: Move units
        Unit unitToMove = decideWhichUnitToMove(gameState);
        if (unitToMove != null) {
            Tile currentTile = gameState.getBoard().getTileForUnit(unitToMove);
            boolean isLosing = gameState.getPlayer2().getAvatar().getCurrentHealth() <
                    gameState.getPlayer1().getAvatar().getCurrentHealth() * 0.75;
            Tile nextTile = calculateBestMove(currentTile, gameState.getPlayer1().getAvatar(), gameState, isLosing);

            if (nextTile != null) {
                gameState.setSelectedUnit(unitToMove); // Set the selected unit
                gameState.setSourceTile(currentTile); // Set the source tile
                gameState.handleMovement(nextTile, unitToMove);
            }
        }

        // Step 3: Attack with units
        attackWithUnits(out, gameState);

        // Step 4: Trigger end turn event processor
        EndTurnClicked endTurnEvent = new EndTurnClicked();
        endTurnEvent.processEvent(out, gameState, null);

    }

    public void selectCardToPlay(GameState gameState) {
        System.out.println("AI's Hand: " + getHand().stream().map(Card::getCardname).collect(Collectors.toList()));
        System.out.println("AI's Mana: " + getMana());

        boolean cardPlayed;
        do {
            cardPlayed = false; // Reset the flag at the start of each iteration
            Card lowestManaCard = null;
            int lowestManaCost = Integer.MAX_VALUE;

            // Iterate through the hand to find the card with the lowest mana cost
            for (Card card : getHand()) {
                System.out.println("Checking card: " + card.getCardname() + " (Mana Cost: " + card.getManacost() + ")");
                if (card.getManacost() < lowestManaCost && card.getManacost() <= getMana()) {
                    // For Sundrop Elixir, check if there are valid targets
                    if (card.getCardname().equals("Sundrop Elixir")) {
                        boolean hasValidTarget = false;
                        for (Tile tile : gameState.getTilesOccupiedByCurrentPlayer()) {
                            Unit unit = gameState.getBoard().getUnitOnTile(tile);
                            if (unit != null && !unit.isAvatar() && unit.getCurrentHealth() < unit.getMaxHealth()) {
                                hasValidTarget = true;
                                break;
                            }
                        }
                        if (!hasValidTarget) {
                            System.out.println("Skipping Sundrop Elixir: No valid targets.");
                            continue; // Skip this card if no valid targets
                        }
                    }

                    lowestManaCost = card.getManacost();
                    lowestManaCard = card;
                }
            }

            if (lowestManaCard != null) {
                System.out.println("Selected card: " + lowestManaCard.getCardname() + " (Mana Cost: " + lowestManaCost + ")");

                if (lowestManaCard.getIsCreature()) {
                    Tile summonTile = selectSummonTile(gameState);
                    if (summonTile != null) {
                        // Set the selected card
                        gameState.setSelectedCard(lowestManaCard);

                        System.out.println("Summoning unit: " + lowestManaCard.getCardname() +
                                " at tile (" + summonTile.getTilex() + "," + summonTile.getTiley() + ")");
                        gameState.handleCreatureCardClick(out, summonTile, lowestManaCard);

                        System.out.println("Deducting " + lowestManaCost + " mana. Remaining mana: " + (getMana() - lowestManaCost));
                        setMana(getMana() - lowestManaCost);
                        getHand().remove(lowestManaCard);

                        cardPlayed = true; // A card was successfully played
                    } else {
                        System.out.println("No valid summon tile found for card: " + lowestManaCard.getCardname());
                    }
                } else {
                    Tile targetTile = selectTargetTile(lowestManaCard, gameState);
                    if (targetTile != null) {
                        System.out.println("Playing spell: " + lowestManaCard.getCardname() +
                                " at tile (" + targetTile.getTilex() + "," + targetTile.getTiley() + ")");
                        gameState.handleSpellCardClick(out, targetTile);

                        System.out.println("Deducting " + lowestManaCost + " mana. Remaining mana: " + (getMana() - lowestManaCost));
                        setMana(getMana() - lowestManaCost);
                        getHand().remove(lowestManaCard);

                        cardPlayed = true; // A card was successfully played
                    } else {
                        System.out.println("No valid target tile found for spell: " + lowestManaCard.getCardname());
                    }
                }

                // Add a small delay to simulate the card being played
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("No card with enough mana.");
            }
        } while (cardPlayed); // Continue only if a card was played in the current iteration
    }

    private Tile selectTargetTile(Card card, GameState gameState) {
        SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(card.getCardname());
        if (spellEffect != null) {
            spellEffect.highlightValidTargets(out, gameState, null);
        }

        List<Tile> validTiles = gameState.getHighlightedTiles();
        if (validTiles.isEmpty()) {
            return null; // No valid tiles, return early
        }

        Random rand = new Random();
        Tile tile = validTiles.get(rand.nextInt(validTiles.size())); // Now size is guaranteed > 0
        return tile;
    }


    private Tile selectSummonTile(GameState gameState) {
        // Highlight valid summon tiles
        gameState.getValidSummonTile(out);
        List<Tile> validTiles = gameState.getHighlightedTiles(); // Assuming this method exists
        if (validTiles.isEmpty()) {
            return null; // No valid tiles available
        }

        // Get the AI's avatar and its health
        Unit aiAvatar = gameState.getPlayer2().getAvatar();
        int aiHealth = aiAvatar.getCurrentHealth();

        // Get the human player's avatar and its health
        Unit humanAvatar = gameState.getPlayer1().getAvatar();
        int humanHealth = humanAvatar.getCurrentHealth();

        // Determine if the AI is in a losing state
        boolean isLosing = aiHealth < humanHealth * 0.75;
        Tile summonTile = null;

        // If the AI is losing, prioritize summoning units closer to its own avatar
        if (isLosing) {
            Tile aiAvatarTile = gameState.getBoard().getTileForUnit(aiAvatar);
            if (aiAvatarTile == null) {
                return null; // AI avatar tile not found
            }

            int minDistance = Integer.MAX_VALUE;
            for (Tile tile : validTiles) {
                int distance = calculateDistance(aiAvatarTile, tile);
                if (distance < minDistance) {
                    minDistance = distance;
                    summonTile = tile;
                }
            }

        } else {
            // Otherwise, prioritize summoning closer to the human avatar
            Tile humanAvatarTile = gameState.getBoard().getTileForUnit(humanAvatar);
            if (humanAvatarTile == null) {
                return null; // Human avatar tile not found
            }

            int minDistance = Integer.MAX_VALUE;
            for (Tile tile : validTiles) {
                int distance = calculateDistance(humanAvatarTile, tile);
                if (distance < minDistance) {
                    minDistance = distance;
                    summonTile = tile;
                }
            }
        }

        return summonTile; // Return the selected summon tile
    }


      // Helper method to calculate distance between two tiles
    private int calculateDistance(Tile tile1, Tile tile2) {
        // Assuming tiles have x and y coordinates
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        // Use Manhattan distance for simplicity
        return dx + dy;
    }

    private Unit decideWhichUnitToMove(GameState gameState) {
        // Get the human player's avatar
        Unit humanAvatar = gameState.getPlayer1().getAvatar();
        Tile humanAvatarTile = gameState.getBoard().getTileForUnit(humanAvatar);
        int humanHealth = humanAvatar.getCurrentHealth();

        // Get the AI's avatar and its health
        Unit aiAvatar = gameState.getPlayer2().getAvatar();
        int aiHealth = aiAvatar.getCurrentHealth();

        // Determine if the AI is in a losing state (e.g., AI has significantly lower health)
        boolean isLosing = aiHealth < humanHealth * 0.75; // AI has less than 75% of human's health

        // Get all units controlled by the AI
        List<Unit> aiUnits = new ArrayList<>();
        for (Tile tile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(tile);
            if (unit != null && !unit.hasMoved()) {
                aiUnits.add(unit);
            }
        }

        // Sort units by strength (attack power) in descending order
        aiUnits.sort((unit1, unit2) -> Integer.compare(unit2.getAttackPower(), unit1.getAttackPower()));

        // Prioritize moving the AI avatar if it's in a losing state
        if (isLosing && !aiAvatar.hasMoved()) {
            Tile aiAvatarTile = gameState.getBoard().getTileForUnit(aiAvatar);
            Tile bestMove = calculateBestMove(aiAvatarTile, humanAvatar, gameState, true); // Move away from human avatar
            if (bestMove != null) {
                gameState.setSelectedUnit(aiAvatar);
                return aiAvatar;
            }
        }

        // Otherwise, move other units towards the human avatar
        for (Unit unit : aiUnits) {
            Tile currentTile = gameState.getBoard().getTileForUnit(unit);
            Tile bestMove = calculateBestMove(currentTile, humanAvatar, gameState, false); // Move towards human avatar
            if (bestMove != null) {
                gameState.setSelectedUnit(unit);
                return unit;
            }
        }

        return null; // No unit to move
    }

    private Tile calculateBestMove(Tile currentTile, Unit targetUnit, GameState gameState, boolean moveAway) {
        int currentX = currentTile.getTilex();
        int currentY = currentTile.getTiley();
        Tile targetTile = gameState.getBoard().getTileForUnit(targetUnit);
        int targetX = targetTile.getTilex();
        int targetY = targetTile.getTiley();

        // Calculate the direction to move
        int dx = Integer.compare(targetX, currentX);
        int dy = Integer.compare(targetY, currentY);

        // If moving away, reverse the direction
        if (moveAway) {
            dx = -dx;
            dy = -dy;
        }

        // Calculate the new position
        int newX = currentX + dx;
        int newY = currentY + dy;

        // Check if the new position is within bounds and not occupied
        if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
            Tile newTile = gameState.getBoard().getTile(newX, newY);
            if (gameState.getBoard().getUnitOnTile(newTile) == null) {
                return newTile;
            }
        }

        // If the direct move is not possible, try adjacent tiles
        int[][] directions = {
                {-1, 0}, {1, 0}, // Left, Right
                {0, -1}, {0, 1}, // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
        };

        for (int[] dir : directions) {
            int adjX = currentX + dir[0];
            int adjY = currentY + dir[1];

            if (adjX >= 0 && adjX < 9 && adjY >= 0 && adjY < 5) {
                Tile adjTile = gameState.getBoard().getTile(adjX, adjY);
                if (gameState.getBoard().getUnitOnTile(adjTile) == null) {
                    return adjTile;
                }
            }
        }

        return null; // No valid move found
    }


    private void attackWithUnits(ActorRef out, GameState gameState) {
        for (Tile tile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(tile);
            if (unit == null || unit.hasAttacked()) continue; // Skip if no unit or already attacked

            // Skip if the unit has already attacked
            if (unit.hasAttacked()) {
                System.out.println("Skipping unit: " + unit.getName() + " (already attacked)");
                continue;
            }

            gameState.getValidAttackTiles(tile); //this methods set attackable tiles in gameStates list
            List<Tile> attackableTiles = gameState.getRedHighlightedTiles(); // Get attackable tiles for this unit
            Unit target = selectBestTarget(unit, attackableTiles, gameState);

            if (target != null) {
                gameState.setSelectedUnit(unit);
                gameState.handleAttack(out, target); // Ensure attack method has the attacker and target
            } else {
                System.out.println("No valid target found for unit: " + unit.getName());
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
