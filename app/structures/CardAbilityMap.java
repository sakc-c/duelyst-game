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
        // Add more cards and abilities as needed
    }

    public static Ability getAbilityForCard(String cardName) {
        return abilities.get(cardName);
    }
}
