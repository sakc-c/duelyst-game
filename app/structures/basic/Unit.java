package structures.basic;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import commands.BasicCommands;
import structures.Ability;
import structures.OnHitEventListener;
import structures.GameState;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile.
 *
 * @author Dr. Richard McCreadie
 */
public class Unit {

    @JsonIgnore
    protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file

    int id;
    UnitAnimationType animation;
    Position position;
    UnitAnimationSet animations;
    ImageCorrection correction;

    @JsonIgnore
    private Player owner;

    private int currentHealth;
    private int maxHealth;
    private int attackPower;
    private boolean isMoved;
    private boolean isAttacked;
    private String name;
    private boolean avatar;
    private boolean stunned = false;
    private List<OnHitEventListener> onHitEventListeners = new ArrayList<>(); // List of event listeners

    @JsonIgnore // Exclude the ability field from serialization
    private List<Ability> abilities = new ArrayList<>();

    private boolean canMove = true;
    private List<Unit> validAttackTargets = new ArrayList<>(); // List of units this unit can attack


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Unit() {
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(0, 0, 0, 0);
        this.correction = correction;
        this.animations = animations;
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
        this.correction = correction;
        this.animations = animations;
    }


    public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations, ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = animation;
        this.position = position;
        this.animations = animations;
        this.correction = correction;
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile, Player owner, int maxHealth, int attackPower) {
        this.id = id;
        this.animation = UnitAnimationType.idle;
        this.position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
        this.animations = animations;
        this.correction = correction;

        // Initialize new attributes
        this.owner = owner;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.attackPower = attackPower;
        this.isMoved = false;
        this.isAttacked = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UnitAnimationType getAnimation() {
        return animation;
    }

    public void setAnimation(UnitAnimationType animation) {
        this.animation = animation;
    }

    public ImageCorrection getCorrection() {
        return correction;
    }

    public void setCorrection(ImageCorrection correction) {
        this.correction = correction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public UnitAnimationSet getAnimations() {
        return animations;
    }

    public void setAnimations(UnitAnimationSet animations) {
        this.animations = animations;
    }

    /**
     * This command sets the position of the Unit to a specified
     * tile.
     *
     * @param tile
     */
    @JsonIgnore
    public void setPositionByTile(Tile tile) {
        position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
    }

    //Additional methods added below

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player player) {
        owner = player;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int health) {
        this.currentHealth = health;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public void setAttackPower(int attack) {
        this.attackPower = attack;
    }


    public void takeDamage(int damage, ActorRef out) {
        // Check if this unit is the player's avatar and if the artifact is equipped
        Player ownerPlayer = this.getOwner();
        if (isAvatar() && ownerPlayer.hasArtifact()) {
            // Reduce artifact robustness instead of player health
            int updatedArtifact = ownerPlayer.getArtifactRobustness() - 1;
            ownerPlayer.equipArtifact(updatedArtifact);

            // If the artifact is destroyed, notify the player
            if (updatedArtifact <= 0) {
                ownerPlayer.setHasArtifact(false); // Remove the artifact
                BasicCommands.addPlayer1Notification(out, "Artifact destroyed!", 2);
            } else {
                BasicCommands.addPlayer1Notification(out, "Artifact robustness reduced to " + updatedArtifact, 2);
            }

        } else {
            // If no artifact or not the avatar, reduce unit health
            this.currentHealth -= damage;
            if (this.currentHealth < 0) {
                this.currentHealth = 0;
            }

            // Update the UI to show the unit's remaining health
            BasicCommands.setUnitHealth(out, this, this.currentHealth);
        }
    }

    public void counterDamage(Unit attacker, ActorRef out) {
        // Example: Deal damage back to the attacker
        attacker.takeDamage(this.attackPower, out);
    }

    public void heal(int amount) {
        currentHealth += amount;
        if (currentHealth > maxHealth) currentHealth = maxHealth;
    }

    public boolean hasMoved() {
        return isMoved;
    }

    public void setHasMoved(boolean b) {
        isMoved = b;
    }

    public boolean hasAttacked() {
        return isAttacked;
    }

    public void setHasAttacked(boolean b) {
        isAttacked = b;
    }

    public void setAbilities(List<Ability> abilitiesList) {
        // Add all abilities in the list to the unit's abilities
        this.abilities.addAll(abilitiesList);
    }

    public List<Ability> getAbilities() {
        return abilities;
    }


    public boolean isAvatar() {
        return avatar;
    }

    public void setAvatar(boolean avatar) {
        this.avatar = avatar;
    }

    public void addOnHitEventListener(OnHitEventListener listener) {
        onHitEventListeners.add(listener);
    }

    public void triggerOnHitEffect(ActorRef out, GameState gameState) {
        for (OnHitEventListener listener : onHitEventListeners) {
            listener.onHit(out, gameState);
        }
    }

    public void setMaximumHealth(int health) {
        this.maxHealth = health;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
    }

    public boolean canMove() {
        return canMove;
    }

    public void setValidAttackTargets(List<Unit> validAttackTargets) {
        this.validAttackTargets = validAttackTargets;
    }

    public boolean canAttack(Unit target) {
        // If no restriction, can attack any unit
        if (validAttackTargets == null || validAttackTargets.isEmpty()) {
            return true;
        }
        // Otherwise, can only attack units in the validAttackTargets list
        return validAttackTargets.contains(target);
    }

    public boolean isStunned() {
        return stunned;
    }

    public void setStunned(boolean stunned) {
        this.stunned = stunned;
    }
}
