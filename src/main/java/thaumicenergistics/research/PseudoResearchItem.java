package thaumicenergistics.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class PseudoResearchItem extends ResearchItem {
    private final ResearchItem real;

    private PseudoResearchItem(String key, String category, int column, int row, TcStack icon, ResearchItem real) {
        super(key, category, new AspectList(), column, row, 1, icon);
        this.real = real;
        setStub().setHidden();
    }

    private PseudoResearchItem(String key, String category, int column, int row, ResourceLocation icon, ResearchItem real) {
        super(key, category, new AspectList(), column, row, 1, icon);
        this.real = real;
        setStub().setHidden();
    }

    public static PseudoResearchItem create(String key, String category, int column, int row, ResearchItem real) {
        PseudoResearchItem pseudo = real.iconItem() != null
            ? new PseudoResearchItem(key, category, column, row, real.iconItem(), real)
            : new PseudoResearchItem(key, category, column, row, real.iconTexture(), real);
        List<String> siblings = new ArrayList<>(real.siblings());
        siblings.add(key);
        real.setSiblings(siblings.toArray(String[]::new));
        return pseudo;
    }

    public ResearchItem real() {
        return real;
    }

    @Override
    public String nameKey() {
        return real.nameKey();
    }

    @Override
    public String textKey() {
        return real.textKey();
    }

    @Override
    public List<ResearchPage> pages() {
        return real.pages();
    }
}
