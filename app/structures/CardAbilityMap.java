package structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps card names to their corresponding abilities.
 * Used to retrieve the appropriate ability objects for each card in the game.
 * Contains static initialization of all card-ability relationships.
 */
public class CardAbilityMap {
    private static final Map<String, List<Ability>> abilities = new HashMap<>();

    static {
        // Populate the map with card names and their abilities
        abilities.put("Bad Omen", new ArrayList<Ability>() {{
            add(new Deathwatch());
        }});
        abilities.put("Shadow Watcher", new ArrayList<Ability>() {{
            add(new Deathwatch());
        }});
        abilities.put("Bloodmoon Priestess", new ArrayList<Ability>() {{
            add(new Deathwatch());
        }});
        abilities.put("Shadowdancer", new ArrayList<Ability>() {{
            add(new Deathwatch());
        }});
        abilities.put("Gloom Chaser", new ArrayList<Ability>() {{
            add(new OpeningGambit());
        }});
        abilities.put("Nightsorrow Assassin", new ArrayList<Ability>() {{
            add(new OpeningGambit());
        }});
        abilities.put("Rock Pulveriser", new ArrayList<Ability>() {{
            add(new Provoke());
        }});
        abilities.put("Young Flamewing", new ArrayList<Ability>() {{
            add(new Flying());
        }});

        // For Silverguard Knight, storing two abilities (ZealAbility and Provoke)
        abilities.put("Silverguard Knight", new ArrayList<Ability>() {{
            add(new ZealAbility());
            add(new Provoke());
        }});

        abilities.put("Saberspine Tiger", new ArrayList<Ability>() {{
            add(new RushAbility());
        }});
        abilities.put("Swamp Entangler", new ArrayList<Ability>() {{
            add(new Provoke());
        }});
        abilities.put("Silverguard Squire", new ArrayList<Ability>() {{
            add(new OpeningGambit());
        }});
        abilities.put("Ironcliffe Guardian", new ArrayList<Ability>() {{
            add(new Provoke());
        }});

        // Add more cards and abilities as needed
    }

    // Get the list of abilities for a specific card
    public static List<Ability> getAbilitiesForCard(String cardName) {
        return abilities.getOrDefault(cardName, new ArrayList<>());
    }
}
