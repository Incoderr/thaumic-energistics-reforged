package thaumicenergistics.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.ThaumicEnergistics;

public final class TeResearch {
    public static final String CATEGORY = ThaumicEnergistics.MODID;
    public static final String BASIC = key("RESEARCH");
    public static final String CORES = key("CORES");
    public static final String STORAGE = key("STORAGE");
    public static final String ESSENTIA_TERMINAL = key("ESSTERM");
    public static final String IO = key("IO");
    public static final String ESSENTIA_PROVIDER = key("ESSPROV");
    public static final String INFUSION_PROVIDER = key("INFPROV");
    public static final String DISTILESSENTIA = "DISTILESSENTIA";
    public static final String WARDED = "WARDEDARCANA";
    public static final String TUBEFILTER = "TUBEFILTER";
    public static final String INFUSION = "INFUSION";
    public static final String MIRROR = "MIRROR";
    public static final int COMPLEXITY_SMALL = 1;
    public static final int COMPLEXITY_MEDIUM = 2;
    public static final int COMPLEXITY_LARGE = 3;

    private static final Map<String, PseudoPosition> PSEUDO_POSITIONS = Map.ofEntries(
        Map.entry("DISTILESSENTIA", new PseudoPosition("ALCHEMY", -2, 0)),
        Map.entry("TUBEFILTER", new PseudoPosition("ALCHEMY", -3, 0)),
        Map.entry("MIRROR", new PseudoPosition("ARTIFICE", -4, 0)),
        Map.entry("JARLABEL", new PseudoPosition("ALCHEMY", -4, 0)),
        Map.entry("INFUSION", new PseudoPosition("ARTIFICE", -6, 0)),
        Map.entry("ALCHEMICALDUPLICATION", new PseudoPosition("ALCHEMY", -5, -6)),
        Map.entry("WARDEDARCANA", new PseudoPosition("ARTIFICE", 1, 2)),
        Map.entry("FOCUSFIRE", new PseudoPosition("THAUMATURGY", -4, -7)),
        Map.entry("VISPOWER", new PseudoPosition("THAUMATURGY", 4, 0)),
        Map.entry("SCEPTRE", new PseudoPosition("THAUMATURGY", 5, 0)),
        Map.entry("COREUSE", new PseudoPosition("GOLEMANCY", 3, -6)),
        Map.entry("COREGATHER", new PseudoPosition("GOLEMANCY", 1, -6))
    );
    private static final Map<String, String> PSEUDO_KEYS = new HashMap<>();

    private record PseudoPosition(String category, int column, int row) {
    }

    private TeResearch() {
    }

    public static void init() {
        ResearchCategories.registerCategory(
            CATEGORY,
            ThaumicEnergistics.id("textures/research/tab.icon.png"),
            ThaumicEnergistics.id("textures/research/research.background.png")
        );
        bound("min", -6, -7);
        bound("max", 6, 2);
    }

    private static void bound(String name, int column, int row) {
        new ResearchItem(ThaumicEnergistics.MODID + ".Bounds." + name, CATEGORY, new AspectList(), column, row, 1, ThaumicEnergistics.id("textures/research/tab.icon.png"))
            .setVirtual()
            .registerResearchItem();
    }

    public static String key(String name) {
        return ThaumicEnergistics.MODID + ".TE" + name;
    }

    public static ResearchPage text(String key, int index) {
        return text(key, Integer.toString(index));
    }

    public static ResearchPage text(String key, String name) {
        return ResearchPage.text(ThaumicEnergistics.MODID + ".research_page." + key.substring(ThaumicEnergistics.MODID.length() + 1) + "." + name);
    }

    public static ResearchPage[] existing(ResearchPage... pages) {
        List<ResearchPage> result = new ArrayList<>();
        for (ResearchPage page : pages) {
            List<String> recipes = switch (page) {
                case ResearchPage.Arcane arcane -> arcane.recipes();
                case ResearchPage.Crafting crafting -> crafting.recipes();
                case ResearchPage.Crucible crucible -> crucible.recipes();
                case ResearchPage.Infusion infusion -> infusion.recipes();
                default -> null;
            };
            if (recipes == null) {
                result.add(page);
                continue;
            }
            String[] present = recipes.stream().filter(key -> TcRecipes.get(key) != null).toArray(String[]::new);
            if (present.length == 0) {
                continue;
            }
            result.add(switch (page) {
                case ResearchPage.Arcane ignored -> ResearchPage.arcane(present);
                case ResearchPage.Crafting ignored -> ResearchPage.crafting(present);
                case ResearchPage.Crucible ignored -> ResearchPage.crucible(present);
                default -> ResearchPage.infusion(present);
            });
        }
        return result.toArray(ResearchPage[]::new);
    }

    public static ResearchItem item(String key, AspectList tags, int column, int row, int complexity, TcStack icon) {
        return new ResearchItem(key, CATEGORY, tags, column, row, complexity, icon);
    }

    public static String pseudo(String realKey) {
        return PSEUDO_KEYS.computeIfAbsent(realKey, TeResearch::registerPseudo);
    }

    private static String registerPseudo(String realKey) {
        PseudoPosition position = PSEUDO_POSITIONS.get(realKey);
        ResearchItem real = ResearchCategories.get(position.category()).get(realKey);
        String key = ThaumicEnergistics.MODID + ".Pseudo." + realKey;
        PseudoResearchItem.create(key, CATEGORY, position.column(), position.row(), real).registerResearchItem();
        return key;
    }
}
