package thaumicenergistics.distillation;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumicenergistics.essentia.EssentiaKey;

public final class DistillationEncoderMenu extends AbstractContainerMenu {
    public static final int BUTTON_ENCODE = 10;
    public static final int SLOT_SOURCE_ITEM_X = 15;
    public static final int SLOT_SOURCE_ITEM_Y = 69;
    public static final int SLOT_SOURCE_ASPECTS_X = 65;
    public static final int SLOT_SOURCE_ASPECTS_Y = 24;
    public static final int SLOT_SELECTED_ASPECT_X = 116;
    public static final int SLOT_SELECTED_ASPECT_Y = 69;
    private static final int SLOT_PATTERNS_BLANK_X = 146;
    private static final int SLOT_PATTERNS_BLANK_Y = 75;
    private static final int SLOT_PATTERN_ENCODED_X = 146;
    private static final int SLOT_PATTERN_ENCODED_Y = 113;
    private static final int PLAYER_INV_Y = 152;
    private static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final Player player;
    private final @Nullable DistillationEncoderBlockEntity encoder;
    private final Slot sourceSlot;
    private final Slot blankSlot;
    private final Slot encodedSlot;
    private final int playerStart;
    private final DataSlot selected = DataSlot.standalone();
    private ItemStack cachedSource = ItemStack.EMPTY;
    private ItemStack cachedPattern = ItemStack.EMPTY;

    public DistillationEncoderMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(DistillationFeature.MENU.get(), containerId);
        this.player = inventory.player;
        this.encoder = player.level().getBlockEntity(pos) instanceof DistillationEncoderBlockEntity found ? found : null;
        Container container = encoder != null ? encoder : new SimpleContainer(3);
        sourceSlot = addSlot(new Slot(container, DistillationEncoderBlockEntity.SLOT_SOURCE_ITEM, SLOT_SOURCE_ITEM_X, SLOT_SOURCE_ITEM_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player clicker) {
                return false;
            }
        });
        blankSlot = addSlot(new Slot(container, DistillationEncoderBlockEntity.SLOT_BLANK_PATTERNS, SLOT_PATTERNS_BLANK_X, SLOT_PATTERNS_BLANK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AEItems.BLANK_PATTERN.is(stack);
            }
        });
        encodedSlot = addSlot(new Slot(container, DistillationEncoderBlockEntity.SLOT_ENCODED_PATTERN, SLOT_PATTERN_ENCODED_X, SLOT_PATTERN_ENCODED_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AEItems.PROCESSING_PATTERN.is(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        playerStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlot(selected);
        selected.set(-1);
    }

    public ItemStack sourceItem() {
        return sourceSlot.getItem();
    }

    public @Nullable List<DistillationAspects.Entry> sourceAspects() {
        return DistillationAspects.of(sourceSlot.getItem(), player);
    }

    public int selectedIndex() {
        return selected.get();
    }

    public @Nullable DistillationAspects.Entry selectedAspect() {
        List<DistillationAspects.Entry> aspects = sourceAspects();
        int index = selected.get();
        return aspects == null || index < 0 || index >= aspects.size() ? null : aspects.get(index);
    }

    private void scanSourceItem(boolean selectFirst) {
        cachedSource = sourceSlot.getItem();
        selected.set(-1);
        List<DistillationAspects.Entry> aspects = sourceAspects();
        if (selectFirst && aspects != null && !aspects.isEmpty() && aspects.get(0).aspect() != null) {
            selected.set(0);
        }
    }

    private void loadPattern() {
        cachedPattern = encodedSlot.getItem();
        if (cachedPattern.isEmpty() || !(PatternDetailsHelper.decodePattern(cachedPattern, player.level()) instanceof AEProcessingPattern pattern)) {
            return;
        }
        List<GenericStack> inputs = pattern.getSparseInputs().stream().filter(stack -> stack != null).toList();
        List<GenericStack> outputs = pattern.getSparseOutputs().stream().filter(stack -> stack != null).toList();
        if (inputs.size() != 1 || outputs.size() != 1 || !(inputs.get(0).what() instanceof AEItemKey item)
            || !(outputs.get(0).what() instanceof EssentiaKey essentia)) {
            return;
        }
        sourceSlot.set(item.toStack());
        scanSourceItem(false);
        List<DistillationAspects.Entry> aspects = sourceAspects();
        if (aspects != null) {
            for (int index = 0; index < aspects.size(); index++) {
                if (aspects.get(index).aspect() == essentia.getAspect()) {
                    selected.set(index);
                }
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (encoder != null && !player.level().isClientSide()) {
            if (encodedSlot.getItem() != cachedPattern) {
                loadPattern();
            } else if (sourceSlot.getItem() != cachedSource) {
                scanSourceItem(true);
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == BUTTON_ENCODE) {
            encode();
            return true;
        }
        List<DistillationAspects.Entry> aspects = sourceAspects();
        if (id >= 0 && aspects != null && id < aspects.size()) {
            selected.set(id);
            return true;
        }
        return false;
    }

    public void encode() {
        DistillationAspects.Entry entry = selectedAspect();
        ItemStack source = sourceSlot.getItem();
        if (entry == null || entry.aspect() == null || source.isEmpty() || !DistillationAspects.canSee(player, entry.aspect())) {
            return;
        }
        boolean takeBlank = encodedSlot.getItem().isEmpty();
        if (takeBlank && blankSlot.getItem().isEmpty()) {
            return;
        }
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
            List.of(new GenericStack(AEItemKey.of(source.copyWithCount(1)), 1)),
            List.of(new GenericStack(EssentiaKey.of(entry.aspect()), entry.amount()))
        );
        encodedSlot.set(pattern);
        cachedPattern = pattern;
        if (takeBlank) {
            blankSlot.remove(1);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player clicker) {
        if (slotIndex == sourceSlot.index) {
            ItemStack carried = getCarried();
            sourceSlot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slotIndex, button, clickType, clicker);
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot == sourceSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        boolean moved;
        if (index >= playerStart) {
            if (blankSlot.mayPlace(stack)) {
                moved = moveItemStackTo(stack, blankSlot.index, blankSlot.index + 1, false);
            } else if (encodedSlot.mayPlace(stack)) {
                moved = !encodedSlot.hasItem() && moveItemStackTo(stack, encodedSlot.index, encodedSlot.index + 1, false);
            } else {
                sourceSlot.set(stack.copyWithCount(1));
                return ItemStack.EMPTY;
            }
        } else {
            moved = moveItemStackTo(stack, playerStart + 27, slots.size(), false) || moveItemStackTo(stack, playerStart, playerStart + 27, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return encoder != null && encoder.stillValid(clicker);
    }

}
