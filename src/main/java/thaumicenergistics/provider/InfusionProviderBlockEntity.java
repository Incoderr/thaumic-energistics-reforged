package thaumicenergistics.provider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSource;
import thaumicenergistics.platform.TeNetwork;

public final class InfusionProviderBlockEntity extends ProviderBlockEntity implements AspectSource {
    public InfusionProviderBlockEntity(BlockPos pos, BlockState state) {
        super(ProviderFeature.INFUSION_PROVIDER_ENTITY.get(), pos, state, 5.0);
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (extractFromNetwork(aspect, amount, true) == amount) {
            if (level instanceof ServerLevel serverLevel) {
                TeNetwork.sendToNear(serverLevel, null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 32.0,
                    new ProviderRunesPayload(worldPosition, aspect.getColor()));
            }
            return true;
        }
        return false;
    }

    @Override
    public AspectList getAspects() {
        return new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return amount;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }
}
