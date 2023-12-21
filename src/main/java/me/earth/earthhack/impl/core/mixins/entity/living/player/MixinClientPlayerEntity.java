package me.earth.earthhack.impl.core.mixins.entity.living.player;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.core.ducks.entity.IClientPlayerEntity;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.movement.autosprint.AutoSprint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends MixinAbstractClientPlayerEntity
        implements IClientPlayerEntity
{
    // private static final ModuleCache<Spectate> SPECTATE =
    //         Caches.getModule(Spectate.class);
    // private static final ModuleCache<ElytraFlight> ELYTRA_FLIGHT =
    //         Caches.getModule(ElytraFlight.class);
    @Unique
    private static final ModuleCache<AutoSprint> SPRINT =
            Caches.getModule(AutoSprint.class);
    // private static final ModuleCache<XCarry> XCARRY =
    //         Caches.getModule(XCarry.class);
    // private static final ModuleCache<Portals> PORTALS =
    //         Caches.getModule(Portals.class);
    // private static final SettingCache<Boolean, BooleanSetting, Portals> CHAT =
    //         Caches.getSetting(Portals.class, BooleanSetting.class, "Chat", true);
    // private static final ModuleCache<Compatibility> ROTATION_BYPASS =
    //         Caches.getModule(Compatibility.class);

    @Shadow
    public Input input;
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private MotionUpdateEvent.Riding riding;
    private MotionUpdateEvent motionEvent = new MotionUpdateEvent();

    @Override
    @Accessor(value = "lastX")
    public abstract double getLastReportedX();

    @Override
    @Accessor(value = "lastBaseY")
    public abstract double getLastReportedY();

    @Override
    @Accessor(value = "lastZ")
    public abstract double getLastReportedZ();

    @Override
    @Accessor(value = "lastYaw")
    public abstract float getLastReportedYaw();

    @Override
    @Accessor(value = "lastPitch")
    public abstract float getLastReportedPitch();

    @Override
    @Accessor(value = "lastOnGround")
    public abstract boolean getLastOnGround();

    @Override
    @Accessor(value = "lastX")
    public abstract void setLastReportedX(double x);

    @Override
    @Accessor(value = "lastBaseY")
    public abstract void setLastReportedY(double y);

    @Override
    @Accessor(value = "lastZ")
    public abstract void setLastReportedZ(double z);

    @Override
    @Accessor(value = "lastYaw")
    public abstract void setLastReportedYaw(float yaw);

    @Override
    @Accessor(value = "lastPitch")
    public abstract void setLastReportedPitch(float pitch);

    @Override
    @Accessor(value = "ticksSinceLastPositionPacketSent")
    public abstract int getPositionUpdateTicks();

    @Override
    @Accessor(value = "mountJumpStrength")
    public abstract void setHorseJumpPower(float jumpPower);

    @Override
    public void superUpdate()
    {
        super.tick();
    }

    @Override
    public void invokeSendMovementPackets()
    {
        this.sendMovementPackets();
    }

    // @Override
    // public boolean isNoInterping()
    // {
    //     return false;
    // }

    @Shadow
    protected abstract void sendMovementPackets();

}