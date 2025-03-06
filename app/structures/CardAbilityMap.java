package structures;

import java.util.HashMap;
import java.util.Map;

public class CardAbilityMap {
    private static final Map<String, Ability> abilities = new HashMap<>();

    static {
        // Populate the map with card names and their abilities
        abilities.put("Bad Omen", new Deathwatch());
        abilities.put("Shadow Watcher", new Deathwatch());
        abilities.put("Bloodmoon Priestess", new Deathwatch());
        abilities.put("Shadowdancer", new Deathwatch());
        abilities.put("Gloom Chaser", new OpeningGambit());
        abilities.put("Nightsorrow Assassin", new OpeningGambit());
        abilities.put("Rock Pulveriser", new Provoke());
        abilities.put("Young Flamewing", new Flying());
        abilities.put("Silverguard Knight", new ZealAbility()); //json file gives name provoke but description of Zeal.
        abilities.put("Saberspine Tiger", new RushAbility());
        abilities.put("Swamp Entangler", new Provoke());
        abilities.put("Silverguard Squire", new OpeningGambit());
        abilities.put("Ironcliffe Guardian", new Provoke());

        // Add more cards and abilities as needed
    }

    public static Ability getAbilityForCard(String cardName) {
        return abilities.get(cardName);
    }
}
