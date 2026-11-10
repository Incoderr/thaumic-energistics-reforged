package thaumicenergistics.golem;

import appeng.api.features.GridLinkables;
import appeng.api.ids.AEComponents;
import appeng.items.parts.FacadeItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.item.golem.GolemBellItem;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeEntityKey;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;

public final class GolemBackpackFeature {
    public static final String BACKPACK_RECIPE = TeRecipes.key("ITEM_GOLEM_WIFI_BACKPACK");
    public static final DeferredItem<WirelessBackpackItem> BACKPACK = ModItems.ITEMS.registerItem("wireless_golem_backpack", WirelessBackpackItem::new, properties -> properties);
    public static final TeEntityKey<BackpackData> DATA = TeEntityKey.persistent("golem_wifi_backpack", () -> new BackpackData(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), ItemStack.EMPTY), BackpackData.CODEC);
    public static final TeEntityKey<BackpackData.Client> CLIENT = TeEntityKey.synced("golem_wifi_backpack_state", () -> BackpackData.Client.NONE, BackpackData.Client.STREAM_CODEC);
    private static final int SYNC_TICKS = 18;
    private static final Map<GolemBase, GolemWifiGoal> GOALS = new WeakHashMap<>();

    private GolemBackpackFeature() {
    }

    public static void load() {
        NeoForge.EVENT_BUS.addListener(GolemBackpackFeature::onInteract);
        NeoForge.EVENT_BUS.addListener(GolemBackpackFeature::onAttack);
        NeoForge.EVENT_BUS.addListener(GolemBackpackFeature::onTick);
    }

    public static void setup() {
        GridLinkables.register(BACKPACK.get(), WirelessTerminalItem.LINKABLE_HANDLER);
    }

    public static BackpackSkin skinOf(ItemStack facade) {
        BackpackSkin skin = facadeSkin(facade);
        return skin == null ? BackpackSkin.THAUMIUM : skin;
    }

    private static @Nullable BackpackSkin facadeSkin(ItemStack facade) {
        if (!(facade.getItem() instanceof FacadeItem item)) {
            return null;
        }
        BlockState state = item.getTextureBlockState(facade);
        return state == null ? null : BackpackSkin.fromBlock(state.getBlock());
    }

    public static boolean attach(GolemBase golem, ItemStack backpack) {
        GlobalPos link = backpack.get(AEComponents.WIRELESS_LINK_TARGET);
        if (link == null || DATA.has(golem) || !GolemWifiGoal.supportsCore(golem.getCore())) {
            return false;
        }
        DATA.set(golem, new BackpackData(link, ItemStack.EMPTY));
        installGoal(golem);
        updateClient(golem);
        return true;
    }

    public static @Nullable GolemWifiGoal goal(GolemBase golem) {
        return GOALS.get(golem);
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GolemBase golem)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        boolean server = !event.getLevel().isClientSide();
        boolean hasBackpack = server ? DATA.has(golem) : CLIENT.has(golem) && CLIENT.get(golem).hasBackpack();
        if (held.getItem() instanceof WirelessBackpackItem) {
            if (hasBackpack || !GolemWifiGoal.supportsCore(golem.getCore())) {
                return;
            }
            if (server && WirelessBackpackItem.isLinked(held) && attach(golem, held)) {
                golem.level().playSound(null, golem, ModSounds.UPGRADE.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
                player.setItemInHand(event.getHand(), ItemStack.EMPTY);
            } else if (!server) {
                player.swing(event.getHand());
            }
            finish(event);
            return;
        }
        BackpackSkin skin = facadeSkin(held);
        if (skin == null || !hasBackpack) {
            return;
        }
        if (!server) {
            player.swing(event.getHand());
            finish(event);
            return;
        }
        BackpackData data = DATA.get(golem);
        if (!data.facade().isEmpty() && !player.getAbilities().instabuild) {
            golem.spawnAtLocation(data.facade(), 0.5F);
        }
        DATA.set(golem, new BackpackData(data.link(), held.copyWithCount(1)));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        updateClient(golem);
        golem.level().playSound(null, golem, ModSounds.CAMERATICKS.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
        finish(event);
    }

    private static void finish(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof GolemBase golem) || golem.level().isClientSide() || !golem.isAlive()) {
            return;
        }
        if (!(event.getEntity().getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GolemBellItem)) {
            return;
        }
        BackpackData data = DATA.get(golem);
        if (data == null) {
            return;
        }
        ItemStack backpack = BACKPACK.toStack();
        backpack.set(AEComponents.WIRELESS_LINK_TARGET, data.link());
        golem.spawnAtLocation(backpack, 0.5F);
        if (!data.facade().isEmpty()) {
            golem.spawnAtLocation(data.facade(), 0.5F);
        }
        DATA.remove(golem);
        GOALS.remove(golem);
        CLIENT.set(golem, BackpackData.Client.NONE);
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof GolemBase golem) || golem.level().isClientSide() || !DATA.has(golem)) {
            return;
        }
        installGoal(golem);
        if (golem.tickCount % (SYNC_TICKS + 1) == 0) {
            updateClient(golem);
        }
    }

    private static void installGoal(GolemBase golem) {
        BackpackData data = DATA.get(golem);
        GolemWifiGoal goal = GOALS.get(golem);
        if (goal != null && golem.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).anyMatch(existing -> existing == goal)) {
            return;
        }
        GOALS.remove(golem);
        GolemWifiGoal created = data == null ? null : GolemWifiGoal.create(golem, data.link());
        if (created != null) {
            golem.goalSelector.addGoal(GolemWifiGoal.priority(golem.getCore()), created);
            GOALS.put(golem, created);
            created.isConnected();
        }
    }

    private static void updateClient(GolemBase golem) {
        BackpackData data = DATA.get(golem);
        GolemWifiGoal goal = GOALS.get(golem);
        BackpackData.Client state = data == null
            ? BackpackData.Client.NONE
            : new BackpackData.Client(goal != null && goal.wasInRange() ? BackpackData.Client.IN_RANGE : BackpackData.Client.OUT_OF_RANGE, data.skin().ordinal());
        if (!state.equals(CLIENT.get(golem))) {
            CLIENT.set(golem, state);
        }
    }

    public static void recipes() {
        TeRecipes.arcane(
            BACKPACK_RECIPE, TeResearch.GOLEM_BACKPACK, TcStack.of(BACKPACK.getId().toString()),
            new AspectList().add(Aspect.AIR, 6).add(Aspect.FIRE, 4).add(Aspect.ORDER, 3),
            new String[] {"TIT", "NWN", "FCF"},
            'T', TcIngredient.item("thaumcraft:thaumium_ingot"),
            'I', TcIngredient.item("ae2:cable_interface"),
            'N', TcIngredient.item("thaumcraft:nitor"),
            'W', TcIngredient.item("ae2:wireless_receiver"),
            'F', TcIngredient.item("ae2:fluix_crystal"),
            'C', TcIngredient.item("ae2:charger")
        );
    }

    public static void research() {
        String key = TeResearch.GOLEM_BACKPACK;
        TeResearch.item(
                key,
                new AspectList().add(Aspect.ENERGY, 5).add(Aspect.AURA, 5).add(Aspect.MIND, 3).add(Aspect.ARMOR, 3).add(Aspect.MOTION, 1).add(Aspect.EXCHANGE, 1),
                0, -6, TeResearch.COMPLEXITY_LARGE, TcStack.of(BACKPACK.getId().toString())
            )
            .setPages(
                TeResearch.text(key, 1),
                ResearchPage.arcane(BACKPACK_RECIPE),
                TeResearch.text(key, 2),
                TeResearch.text(key, 3),
                TeResearch.text(key, "COREGATHER"),
                concealed(key, "COREFILL"),
                concealed(key, "CORELIQUID"),
                concealed(key, "COREALCHEMY"),
                concealed(key, "UPGRADEORDER"),
                concealed(key, "ADVANCEDGOLEM")
            )
            .setParents(TeResearch.CORES, TeResearch.COREGATHER, TeResearch.pseudo(TeResearch.COREGATHER))
            .setConcealed()
            .registerResearchItem();
    }

    private static ResearchPage concealed(String key, String research) {
        ResearchPage.Text text = (ResearchPage.Text) TeResearch.text(key, research);
        return ResearchPage.concealed(research, text.text());
    }

}
