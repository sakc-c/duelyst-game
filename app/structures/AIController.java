package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import events.EndTurnClicked;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.OrderedCardLoader;

import java.util.*;
import java.util.stream.Collectors;

public class AIController extends Player {
    private final List<Card> deck;  // All available cards
    private final ActorRef out;

    public AIController(int health, int mana, ActorRef out) {
        super(health, mana);
        this.deck = OrderedCardLoader.getPlayer2Cards(2);
        Collections.shuffle(this.deck);
        this.out = out;
    }

    public void drawInitialHand(GameState gameState) {
        for (int i = 0; i < 3; i++) {
            drawCard(gameState);
        }
    }

    /**
     * Draws a card from the AI's deck and adds it to the hand.
     * If the hand is full, the drawn card is discarded.
     * If the deck is empty and the hand is also empty, the game ends.
     *
     * @param gameState The current game state.
     */
    public void drawCard(GameState gameState) {
        if (getHand().size() < 6 && !deck.isEmpty()) {
            Card newCard = deck.remove(0);
            getHand().add(newCard);
        } else if (!deck.isEmpty()) {
            deck.remove(0); //regardless, player loses their card
        }
        if (deck.isEmpty() && getHand().isEmpty()) {//if deck is empty and hand is empty too, game over
            BasicCommands.addPlayer1Notification(out, "Deck finished", 2);
            Player winner = gameState.getPlayer1();
            gameState.endGame(winner, out);
        }

    }

    /**
     * Executes the AI's turn, which involves:
     * - Playing a card from the AI's hand.
     * - Moving units based on the current game state.
     * - Attacking with units.
     * - Triggering the end-turn event.
     *
     * @param card      The card to be played by the AI.
     * @param out       The ActorRef used for sending UI updates.
     * @param gameState The current game state.
     */
    public void playCard(Card card, ActorRef out, GameState gameState) {
        // Step 1: Play a card and summon if possible
        selectCardToPlay(gameState);

        try {
            Thread.sleep(1000); // 500ms delay
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Step 2: Move units
        Unit unitToMove = decideWhichUnitToMove(gameState);
        if (unitToMove != null) {
            Tile currentTile = gameState.getBoard().getTileForUnit(unitToMove);
            boolean isLosing = gameState.getPlayer2().getAvatar().getCurrentHealth() < gameState.getPlayer1().getAvatar().getCurrentHealth() * 0.75;
            Tile nextTile = calculateBestMove(currentTile, gameState.getPlayer1().getAvatar(), gameState, isLosing);

            if (nextTile != null) {
                gameState.setSelectedUnit(unitToMove); // Set the selected unit
                gameState.setSourceTile(currentTile); // Set the source tile
                gameState.handleMovement(out, nextTile, unitToMove);
            }
        }

        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Step 3: Attack with units
        attackWithUnits(out, gameState);

        try {
            Thread.sleep(1000); // 500ms delay
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Step 4: Trigger end turn event processor
        EndTurnClicked endTurnEvent = new EndTurnClicked();
        endTurnEvent.processEvent(out, gameState, null);

    }

    public void selectCardToPlay(GameState gameState) {
        System.out.println("AI's Hand: " + getHand().stream().map(Card::getCardname).collect(Collectors.toList()));
        System.out.println("AI's Mana: " + getMana());

        boolean cardPlayed;
        do {
            cardPlayed = false;
            Card cardToPlay = findBestPlayableCard(gameState);

            if (cardToPlay != null) {
                if (cardToPlay.getIsCreature()) {
                    cardPlayed = playCreatureCard(cardToPlay, gameState);
                } else {
                    cardPlayed = playSpellCard(cardToPlay, gameState);
                }

                if (cardPlayed) {
                    updateAfterCardPlayed(cardToPlay);
                    // Small delay between card plays
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } while (cardPlayed);
    }

    /**
     * Finds the best card that the AI can play based on its current mana and game state.
     * Skips cards that cannot be played due to mana restrictions or invalid targets.
     *
     * @param gameState The current game state.
     * @return The best playable card.
     */
    private Card findBestPlayableCard(GameState gameState) {
        Card lowestManaCard = null;
        int lowestManaCost = Integer.MAX_VALUE;

        for (Card card : getHand()) {
            if (card.getManacost() > getMana()) {
                continue; //skip, not enought mana
            }

            if (!isCardPlayable(card, gameState)) {
                continue;
            }

            if (card.getManacost() < lowestManaCost) {
                lowestManaCost = card.getManacost();
                lowestManaCard = card;
            }
        }

        return lowestManaCard;
    }

    private boolean isCardPlayable(Card card, GameState gameState) {
        if (card.getIsCreature()) {
            return true; // We'll check for summon tiles later
        } else {
            SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(card.getCardname());
            if (spellEffect != null) {
                spellEffect.highlightValidTargets(out, gameState, null);

                if (gameState.getHighlightedTiles().isEmpty() && gameState.getRedHighlightedTiles().isEmpty()) {
                    System.out.println("Skipping spell card: " + card.getCardname() + " (No valid targets)");
                    return false;
                }
                return true;
            } else {
                System.out.println("Skipping spell card: " + card.getCardname() + " (No spell effect found)");
                return false;
            }
        }
    }

    private boolean playCreatureCard(Card card, GameState gameState) {
        Tile summonTile = selectSummonTile(gameState);
        if (summonTile != null) {
            gameState.setSelectedCard(card);
            gameState.handleCreatureCardClick(out, summonTile, card);
            return true;
        } else {
            return false;
        }
    }

    private boolean playSpellCard(Card card, GameState gameState) {
        Tile targetTile = selectTargetTile(card, gameState);
        if (targetTile != null) {
            System.out.println("Target Tile: (" + targetTile.getTilex() + "," + targetTile.getTiley() + ")");
            gameState.addRedHighlightedTile(targetTile);
            gameState.setSelectedCard(card);

            try {
                Thread.sleep(500); // Slight delay before playing spell
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Playing spell: " + card.getCardname() + " at tile (" + targetTile.getTilex() + "," + targetTile.getTiley() + ")");
            gameState.handleSpellCardClick(out, targetTile);

            return true;
        } else {
            System.out.println("No valid target tile found for spell: " + card.getCardname());
            return false;
        }
    }

    private void updateAfterCardPlayed(Card card) {
        setMana(getMana() - card.getManacost());
        getHand().remove(card);
    }

    /**
     * Selects the target tile for a spell card.
     * Identifies valid target tiles and selects the best target based on the spell type.
     * The AI prioritizes certain units based on their health, attack power, or other factors.
     *
     * @param card      The card being played, which defines the type of spell.
     * @param gameState The current game state.
     * @return The selected target tile for the spell, or {@code null} if no valid target is found.
     */
    private Tile selectTargetTile(Card card, GameState gameState) {
        SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(card.getCardname());
        if (spellEffect != null) {
            spellEffect.highlightValidTargets(out, gameState, null);
        }

        List<Tile> validTiles = gameState.getRedHighlightedTiles();
        if (validTiles.isEmpty()) {
            return null; // No valid tiles, return early
        }

        // If only one target is available, choose it
        if (validTiles.size() == 1) {
            return validTiles.get(0);
        }

        // Strategic targeting based on spell type
        String cardName = card.getCardname();
        switch (cardName) {
            case "Sundrop Elixir":
                return selectSundropElixirTarget(validTiles);
            case "Truestrike":
                return selectTrueStrikeTarget(validTiles);
            case "Beamshock":
                return selectBeamShockTarget(validTiles);
            default:
                // Default to random selection
                Random rand = new Random();
                return validTiles.get(rand.nextInt(validTiles.size()));
        }
    }

    // Target unit with highest attack power that is damaged
    private Tile selectSundropElixirTarget(List<Tile> validTiles) {
        Tile bestTile = null;
        int bestScore = -1;

        for (Tile tile : validTiles) {
            Unit unit = tile.getUnit();
            if (unit != null) {
                // Skip units at full health
                if (unit.getCurrentHealth() >= unit.getMaxHealth()) {
                    continue;
                }

                // Score based on attack power
                int score = unit.getAttackPower();

                // Prioritize the avatar more
                if (unit.isAvatar()) {
                    score += 100;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestTile = tile;
                }
            }
        }

        // If no damaged units found, pick the first tile
        return bestTile != null ? bestTile : validTiles.get(0);
    }

    // Target units that would die from 2 damage
    private Tile selectTrueStrikeTarget(List<Tile> validTiles) {
        // First priority: Units that would die from 2 damage
        for (Tile tile : validTiles) {
            Unit unit = tile.getUnit();
            if (unit != null && unit.getCurrentHealth() <= 2) {
                return tile;
            }
        }

        // Second priority: Enemy avatar
        for (Tile tile : validTiles) {
            Unit unit = tile.getUnit();
            if (unit != null && unit.isAvatar()) {
                return tile;
            }
        }

        // Third priority: Unit with highest attack power
        Tile highestAttackTile = null;
        int highestAttack = -1;

        for (Tile tile : validTiles) {
            Unit unit = tile.getUnit();
            if (unit != null && unit.getAttackPower() > highestAttack) {
                highestAttack = unit.getAttackPower();
                highestAttackTile = tile;
            }
        }

        return highestAttackTile != null ? highestAttackTile : validTiles.get(0);
    }

    // Target enemy unit with highest attack power
    private Tile selectBeamShockTarget(List<Tile> validTiles) {
        Tile bestTile = null;
        int highestAttack = -1;

        for (Tile tile : validTiles) {
            Unit unit = tile.getUnit();
            if (unit != null) {
                // Skip units that already attacked
                if (unit.hasAttacked()) {
                    continue;
                }

                if (unit.getAttackPower() > highestAttack) {
                    highestAttack = unit.getAttackPower();
                    bestTile = tile;
                }
            }
        }

        // If no suitable target found, pick the first tile
        return bestTile != null ? bestTile : validTiles.get(0);
    }


    /**
     * Selects the best tile for summoning a unit, considering the AI's current state.
     * If the AI is losing, it prioritizes summoning units near its own avatar.
     *
     * @param gameState The current game state.
     * @return The best tile to summon a unit, or {@code null} if no valid tiles are found.
     */
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
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        return dx + dy;
    }

    /**
     * Decides which unit the AI should move based on the current game state.
     * The AI considers its health relative to the opponent’s health and selects the best unit to move.
     * If the AI is losing, it may prioritize moving its avatar or other high-priority units.
     *
     * @param gameState The current game state.
     * @return The unit to move, or {@code null} if no suitable unit is found.
     */
    private Unit decideWhichUnitToMove(GameState gameState) {
        // Get the human player's avatar
        Unit humanAvatar = gameState.getPlayer1().getAvatar();
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
            if (bestMove != null && unit.canMove()) {
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

        // Get valid movement tiles (this updates highlightedTiles list in GameState)
        gameState.getValidMovementTiles(currentX, currentY, null);
        List<Tile> movementTiles = gameState.getHighlightedTiles();

        if (movementTiles.isEmpty()) return null; // No valid moves

        // Find the best tile based on distance
        return movementTiles.stream().min(Comparator.comparingInt(tile -> moveAway ? calculateDistance(tile, targetTile) * -1 : // Move away increases distance
                        calculateDistance(tile, targetTile))) // Move towards decreases distance
                .orElse(null);
    }

    /**
     * Determines which units the AI will attack and performs the attacks.
     * Loops through all units controlled by the AI and selects valid attack targets.
     *
     * @param out       The ActorRef used for sending UI updates.
     * @param gameState The current game state.
     */
    private void attackWithUnits(ActorRef out, GameState gameState) {
        gameState.clearAllHighlights(out);
        for (Tile tile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(tile);

            // Skip if no unit or already attacked
            if (unit == null || unit.hasAttacked()) {
                continue;
            }

            gameState.getValidAttackTiles(tile); // Get the attackable tiles for this unit
            try {
                Thread.sleep(400); // 1 second delay
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<Tile> attackableTiles = gameState.getRedHighlightedTiles(); // Get attackable tiles for this unit

            // Check if there are valid attackable tiles, if not stop
            if (attackableTiles == null || attackableTiles.isEmpty()) {
                continue;
            }

            // Select the best target for the unit
            Unit target = selectBestTarget(unit, attackableTiles, gameState);

            // If a target is found, proceed with the attack
            if (target != null) {
                gameState.setSelectedUnit(unit); //to set as an attacker in gameState, used by handleAttack method called next
                gameState.handleAttack(out, target);
            } else {
                System.out.println("No valid target found for unit: " + unit.getName());
            }
            gameState.clearAllHighlights(out);
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Unit selectBestTarget(Unit attacker, List<Tile> attackableTiles, GameState gameState) {
        Unit bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        for (Tile attackTile : attackableTiles) {
            Unit possibleTarget = gameState.getBoard().getUnitOnTile(attackTile);
            if (possibleTarget == null || !attacker.canAttack(possibleTarget))
                continue; // Skip empty or invalid targets

            int score = calculateTargetScore(attacker, possibleTarget);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = possibleTarget;
            }
        }
        return bestTarget;
    }

    /**
     * Calculates a score for a target unit based on several factors:
     * - Prioritizes avatar units.
     * - Targets low-health units and high-attack units.
     * - Avoids suicidal attacks where the AI unit would die.
     *
     * @param attacker The attacking unit.
     * @param target   The target unit.
     * @return A score representing the desirability of attacking the target.
     */
    private int calculateTargetScore(Unit attacker, Unit target) {
        int score = 0;

        // Prioritize attacking the opponent's avatar
        if (target.isAvatar()) {
            score += 1000;
        }

        // Prioritize low-health targets to eliminate them quickly
        score += (10 - target.getCurrentHealth()) * 10;

        // Prioritize high-value targets (strong attack power)
        score += target.getAttackPower() * 5;

        // Avoid suicidal attacks where AI unit would die
        if (attacker.getCurrentHealth() <= target.getAttackPower()) {
            score -= 1000;
        }

        return score;
    }


}
