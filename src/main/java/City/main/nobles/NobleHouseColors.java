// NobleHouseColors.java
package City.main.nobles;

import java.awt.Color;
import java.util.Map;

import City.main.parameters.BankParams;

/**
 * Two-color palette for each noble house used in political map view.
 * Primary = zone fill. Secondary = stripe overlay.
 */
public final class NobleHouseColors {

    private NobleHouseColors() {}

    private static final Color DEFAULT_PRIMARY   = new Color(60, 55, 70);
    private static final Color DEFAULT_SECONDARY = new Color(80, 75, 90);

    private static final Map<String, Color[]> COLORS = Map.ofEntries(
        Map.entry("house_valdris",     new Color[]{ new Color(180, 40,  40),  new Color(220, 180, 60)  }),
        Map.entry("house_thornmere",   new Color[]{ new Color(40,  120, 180), new Color(180, 230, 255) }),
        Map.entry("house_ashkar",      new Color[]{ new Color(60,  130, 50),  new Color(200, 160, 40)  }),
        Map.entry("house_deepvein",    new Color[]{ new Color(120, 80,  40),  new Color(200, 200, 200) }),
        Map.entry("house_crestfall",   new Color[]{ new Color(160, 60,  160), new Color(220, 180, 255) }),
        Map.entry("house_sylvaine",    new Color[]{ new Color(30,  160, 120), new Color(180, 255, 200) }),
        Map.entry("house_duskmantle",  new Color[]{ new Color(60,  60,  130), new Color(180, 180, 255) }),
        Map.entry("house_saltborn",    new Color[]{ new Color(180, 120, 40),  new Color(100, 180, 220) }),
        Map.entry("house_emberveil",   new Color[]{ new Color(200, 80,  20),  new Color(255, 200, 100) }),
        Map.entry("house_varlow",      new Color[]{ new Color(90,  100, 110), new Color(225, 205, 150) }),
        Map.entry("house_korrath",     new Color[]{ new Color(115, 55,  45),  new Color(205, 195, 175) }),
        Map.entry("house_wrenfeld",    new Color[]{ new Color(55,  75,  115), new Color(195, 200, 205) }),
        Map.entry("house_stillwater",  new Color[]{ new Color(35,  135, 145), new Color(190, 230, 220) }),
        Map.entry("house_ashgrave",    new Color[]{ new Color(75,  65,  60),  new Color(230, 120, 60)  }),
        Map.entry("house_mournhollow", new Color[]{ new Color(75,  45,  65),  new Color(175, 165, 175) }),
        Map.entry("house_brackenwood", new Color[]{ new Color(50,  100, 60),  new Color(175, 210, 150) }),
        Map.entry("house_tallowmere",  new Color[]{ new Color(60,  90,  120), new Color(235, 210, 150) }),
        Map.entry("house_greyfen",     new Color[]{ new Color(85,  90,  60),  new Color(180, 170, 90)  }),
        Map.entry("house_quickstone",  new Color[]{ new Color(100, 100, 105), new Color(195, 135, 85)  }),
        Map.entry("house_larkspur",    new Color[]{ new Color(135, 110, 160), new Color(235, 190, 210) }),
        Map.entry("house_emberlight",  new Color[]{ new Color(175, 75,  95),  new Color(250, 195, 165) }),
        Map.entry("house_marrow",      new Color[]{ new Color(115, 75,  55),  new Color(205, 190, 160) }),
        Map.entry("house_hollowmere",  new Color[]{ new Color(80,  60,  45),  new Color(175, 190, 150) }),
        Map.entry("house_corvane",     new Color[]{ new Color(75,  95,  115), new Color(165, 55,  55)  }),
        Map.entry(BankParams.BANK_HOUSE_ID, new Color[]{ new Color(75, 115, 155), new Color(225, 190, 90) })
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