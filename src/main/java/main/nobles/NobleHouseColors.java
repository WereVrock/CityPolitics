// NobleHouseColors.java
package main.nobles;

import java.awt.Color;
import java.util.Map;

/**
 * Two-color palette for each noble house used in political map view.
 * Primary = zone fill. Secondary = stripe overlay.
 */
public final class NobleHouseColors {

    private NobleHouseColors() {}

    private static final Color DEFAULT_PRIMARY   = new Color(60, 55, 70);
    private static final Color DEFAULT_SECONDARY = new Color(80, 75, 90);

    private static final Map<String, Color[]> COLORS = Map.ofEntries(
        Map.entry("house_valdris",    new Color[]{ new Color(180, 40,  40),  new Color(220, 180, 60)  }),
        Map.entry("house_thornmere",  new Color[]{ new Color(40,  120, 180), new Color(180, 230, 255) }),
        Map.entry("house_ashkar",     new Color[]{ new Color(60,  130, 50),  new Color(200, 160, 40)  }),
        Map.entry("house_deepvein",   new Color[]{ new Color(120, 80,  40),  new Color(200, 200, 200) }),
        Map.entry("house_crestfall",  new Color[]{ new Color(160, 60,  160), new Color(220, 180, 255) }),
        Map.entry("house_sylvaine",   new Color[]{ new Color(30,  160, 120), new Color(180, 255, 200) }),
        Map.entry("house_duskmantle", new Color[]{ new Color(60,  60,  130), new Color(180, 180, 255) }),
        Map.entry("house_saltborn",   new Color[]{ new Color(180, 120, 40),  new Color(100, 180, 220) }),
        Map.entry("house_emberveil",  new Color[]{ new Color(200, 80,  20),  new Color(255, 200, 100) })
    );

    public static Color getPrimary(String houseId) {
        Color[] pair = COLORS.get(houseId);
        return pair != null ? pair[0] : DEFAULT_PRIMARY;
    }

    public static Color getSecondary(String houseId) {
        Color[] pair = COLORS.get(houseId);
        return pair != null ? pair[1] : DEFAULT_SECONDARY;
    }
}