package me.earth.earthhack.impl.modules.combat.snowballer;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.util.math.RayTraceUtil;
import me.earth.earthhack.impl.util.math.StopWatch;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.network.NetworkUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

import java.util.*;
import java.util.stream.Collectors;

/**
 * I AM SO SMART IT HURTS
 *
 * @author MEGYN
 */
public class Snowballer extends Module {

    protected float[] rotations;
    protected Entity target = null;
    protected Set<Integer> blackList = new HashSet<>();
    protected StopWatch timer = new StopWatch();
    private int lastSlot;

    private boolean shouldThrow;

    final Setting<Float> range           =
            register(new NumberSetting<>("Range", 6.0f, 1.0f, 6.0f));
    final Setting<Integer> delay           =
            register(new NumberSetting<>("Delay", 50, 0, 500));
    final Setting<Boolean> swap          =
            register(new BooleanSetting("Swap", true));
    final Setting<Boolean> back          =
            register(new BooleanSetting("SwapBack", true));
    final Setting<Boolean> blacklist     =
            register(new BooleanSetting("Blacklist", true));

    public Snowballer()
    {
        super("Snowballer", Category.Combat);
        this.listeners.add(new ListenerMotion(this));
        timer.reset();
    }

    protected void runPre(MotionUpdateEvent event) {
        if (timer.passed(delay.getValue()) || delay.getValue() == 0) {
            if (swap.getValue() || InventoryUtil.isHolding(Items.SNOWBALL)) {
                int slot = InventoryUtil.findHotbarItem(Items.SNOWBALL);
                lastSlot = mc.player.getInventory().selectedSlot;
                List<Entity> entities = getCrystals(range.getValue());
                for (Entity entity : entities) {
                    if (!RayTraceUtil.canBeSeen(entity, mc.player) || blackList.contains(entity.getId())) continue;
                    target = entity;
                }
                if (target != null) {
                    if (swap.getValue() && slot != -1 && !InventoryUtil.isHolding(Items.SNOWBALL)) {
                        InventoryUtil.switchTo(slot);
                    }
                    rotations = RotationUtil.getRotations(target);
                    event.setYaw(rotations[0]);
                    event.setPitch(rotations[1]);
                    if (blacklist.getValue()) {
                        blackList.add(target.getId());
                    }
                    target = null;
                    shouldThrow = true;
                }
            }
        }
    }

    protected void runPost(MotionUpdateEvent event) {
        if (shouldThrow && (timer.passed(delay.getValue()) || delay.getValue() == 0) && InventoryUtil.isHolding(Items.SNOWBALL)) {
            shouldThrow = false;
            boolean offhand = mc.player.getOffHandStack().getItem() == Items.SNOWBALL;
            NetworkUtil.sendSequenced(seq -> new PlayerInteractItemC2SPacket(offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, seq, event.getYaw(), event.getPitch()));
            if (swap.getValue() && back.getValue()) {
                InventoryUtil.switchTo(lastSlot);
            }
            timer.reset();
        }
    }

    protected List<Entity> getCrystals(float range) {
        List<Entity> loadedEntities = new ArrayList<>();
        mc.world.getEntities().forEach(loadedEntities::add);
        return loadedEntities.stream()
                .filter(entity -> mc.player.squaredDistanceTo(entity) <= range * range)
                .filter(entity -> entity instanceof EndCrystalEntity)
                .sorted(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)))
                .collect(Collectors.toList());
    }

}