package me.earth.earthhack.impl.modules.combat.surround;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.helpers.blocks.modes.PlaceSwing;
import me.earth.earthhack.impl.util.helpers.blocks.modes.RayTraceMode;
import me.earth.earthhack.impl.util.helpers.blocks.modes.Rotate;
import me.earth.earthhack.impl.util.math.RayTraceUtil;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.Swing;
import me.earth.earthhack.impl.util.minecraft.blocks.BlockUtil;
import me.earth.earthhack.impl.util.network.NetworkUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// TODO: handle weakness...
final class ListenerSpawnObject extends
        ModuleListener<Surround, PacketEvent.Receive<EntitySpawnS2CPacket>>
{
    public ListenerSpawnObject(Surround module)
    {
        super(module, PacketEvent.Receive.class, EntitySpawnS2CPacket.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<EntitySpawnS2CPacket> event)
    {
        if (!module.predict.getValue()
            || module.rotate.getValue() == Rotate.Normal
            || Managers.SWITCH.getLastSwitch() < module.cooldown.getValue())
        {
            return;
        }

        EntitySpawnS2CPacket packet = event.getPacket();
        if (packet.getEntityData() != 51)
        {
            return;
        }

        PlayerEntity player = module.getPlayer();
        BlockPos pos = new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ());
        if (player.squaredDistanceTo(pos.toCenterPos()) < 9)
        {
            if (!module.async.getValue()
                    || DamageUtil.isWeaknessed()
                    || module.smartRay.getValue() != RayTraceMode.Fast
                    || !module.timer.passed(module.delay.getValue())
                    || !module.pop.getValue().shouldPop(
                            DamageUtil.calculate(pos.down(), player),
                            module.popTime.getValue()))
            {
                event.addPostEvent(() -> ListenerMotion.start(module));
                return;
            }

            try
            {
                placeAsync(packet, player);
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    private void placeAsync(EntitySpawnS2CPacket packet, PlayerEntity player)
    {
        int slot = InventoryUtil.findHotbarBlock(Blocks.OBSIDIAN,
                                                 Blocks.ENDER_CHEST);
        if (slot == -1)
        {
            return;
        }

        Box bb = new EndCrystalEntity(mc.world,
                                      packet.getX(),
                                      packet.getY(),
                                      packet.getZ())
                                .getBoundingBox();

        Set<BlockPos> surrounding = module.createSurrounding(
                module.createBlocked(),
                Managers.ENTITIES.getPlayers());

        Map<BlockPos, Direction> toPlace = new ConcurrentHashMap<>();
        for (BlockPos pos : surrounding)
        {
            if (bb.intersects(new Box(pos))
                    && mc.world.getBlockState(pos)
                               .isReplaceable())
            {
                // TODO: smart raytrace here
                Direction facing = BlockUtil.getFacing(pos, mc.world, module.smartRay.getValue().equals(RayTraceMode.Direction));
                if (facing != null)
                {
                    toPlace.put(pos.offset(facing), facing.getOpposite());
                }
            }
        }

        if (toPlace.isEmpty())
        {
            return;
        }

        List<BlockPos> placed = new ArrayList<>(
                Math.min(module.blocks.getValue(), toPlace.size()));

        Locks.acquire(Locks.PLACE_SWITCH_LOCK, () ->
        {
            int lastSlot = mc.player.getInventory().selectedSlot;
            PacketUtil.attack(Managers.ENTITIES.getEntity(packet.getEntityId()));
            InventoryUtil.switchTo(slot);
            boolean sneaking = mc.player.isSneaking();
            if (!sneaking)
            {
                PacketUtil.sneak(true);
            }

            int blocks = 0;
            for (Map.Entry<BlockPos, Direction> entry : toPlace.entrySet())
            {
                float[] helpingRotations = RotationUtil.getRotations(
                        entry.getKey(), entry.getValue(), player);

                BlockHitResult result =
                    RayTraceUtil.getBlockHitResultWithEntity(
                        helpingRotations[0], helpingRotations[1], player);

                if (module.rotate.getValue() == Rotate.Packet)
                {
                    PacketUtil.doRotation(helpingRotations[0], helpingRotations[1], mc.player.isOnGround());
                    /*
                    PingBypass.sendToActualServer(
                            new CPacketPlayer.Rotation(helpingRotations[0],
                                                       helpingRotations[1],
                                                       mc.player.isOnGround()));
                     */
                }

                float[] f = RayTraceUtil.hitVecToPlaceVec(
                        entry.getKey(), result.getPos());

                NetworkUtil.sendSequenced(seq -> new PlayerInteractBlockC2SPacket(
                        InventoryUtil.getHand(slot),
                        new BlockHitResult(new Vec3d(f[0], f[1], f[2]), entry.getValue(), entry.getKey(), false),
                        seq));

                if (module.placeSwing.getValue() == PlaceSwing.Always)
                {
                    Swing.Packet.swing(InventoryUtil.getHand(slot));
                }

                placed.add(entry.getKey().offset(entry.getValue()));
                if (++blocks >= module.blocks.getValue())
                {
                    break;
                }
            }

            if (module.placeSwing.getValue() == PlaceSwing.Once)
            {
                Swing.Packet.swing(InventoryUtil.getHand(slot));
            }

            if (!sneaking)
            {
                PacketUtil.sneak(false);
            }

            InventoryUtil.switchTo(lastSlot);
        });

        module.timer.reset(module.delay.getValue());

        if (module.resync.getValue())
        {
            mc.execute(() ->
            {
                module.placed.addAll(placed);
            });
        }
    }

}
