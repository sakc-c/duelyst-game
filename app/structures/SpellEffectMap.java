package structures;

import java.util.HashMap;
import java.util.Map;

public class SpellEffectMap {
    private static final Map<String, SpellEffect> spells = new HashMap<>();

    static {
        // Populate the map with spell card names and their effects
        spells.put("Wraithling Swarm", new WraithlingSwarm());
        spells.put("Dark Terminus", new DarkTerminusEffect());
        spells.put("Horn of the Forsaken", new HornOfTheForsaken());
        spells.put("True Strike", new TrueStrike());
        // Add more spell cards and their effects here
    }

    public static SpellEffect getSpellEffectForCard(String cardName) {
        return spells.get(cardName);
    }
}