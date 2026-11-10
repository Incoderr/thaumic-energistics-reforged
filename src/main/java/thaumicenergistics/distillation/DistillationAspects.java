package thaumicenergistics.distillation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ScanKind;
import thaumcraft.research.ScanManager;

public final class DistillationAspects {
    public static final int SLOT_COUNT = 6;

    public record Entry(@Nullable Aspect aspect, int amount) {
    }

    private DistillationAspects() {
    }

    public static boolean canSee(Player player, Aspect aspect) {
        return Knowledge.get(player).hasDiscoveredAspect(aspect);
    }

    public static @Nullable List<Entry> of(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return null;
        }
        AspectList aspects = AspectHelper.getBonusTags(stack);
        if (aspects == null || aspects.size() == 0) {
            return null;
        }
        List<Entry> entries = new ArrayList<>();
        if (!Knowledge.get(player).isScanned(ScanKind.OBJECT, ScanManager.itemKey(stack))) {
            for (int index = 0; index < SLOT_COUNT; index++) {
                entries.add(new Entry(null, 0));
            }
            return entries;
        }
        for (Aspect aspect : aspects.getAspectsSortedAmount()) {
            if (entries.size() >= SLOT_COUNT) {
                break;
            }
            entries.add(new Entry(aspect, aspects.getAmount(aspect)));
        }
        return entries;
    }
}
