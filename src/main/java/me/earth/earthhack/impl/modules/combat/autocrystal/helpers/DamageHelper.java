package me.earth.earthhack.impl.modules.combat.autocrystal.helpers;

import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.autocrystal.AutoCrystal;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.MotionTracker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.WorldAccess;

public class DamageHelper implements Globals
{
    private final Setting<Boolean> terrainCalc;
    private final Setting<Integer> bExtrapolation;
    private final Setting<Integer> pExtrapolation;
    private final Setting<Boolean> selfExtrapolation;
    private final Setting<Boolean> obbyTerrain;
    private final ExtrapolationHelper positionHelper;
    private final AutoCrystal module;

    public DamageHelper(AutoCrystal module,
                        ExtrapolationHelper positionHelper,
                        Setting<Boolean> terrainCalc,
                        Setting<Integer> extrapolation,
                        Setting<Integer> bExtrapolation,
                        Setting<Boolean> selfExtrapolation,
                        Setting<Boolean> obbyTerrain)
    {
        this.module = module;
        this.positionHelper = positionHelper;
        this.terrainCalc       = terrainCalc;
        this.pExtrapolation    = extrapolation;
        this.bExtrapolation    = bExtrapolation;
        this.selfExtrapolation = selfExtrapolation;
        this.obbyTerrain       = obbyTerrain;
    }

    // Break damage for ourselves //
    public float getDamage(Entity crystal)
    {
        if (module.isSuicideModule())
        {
            return 0.0f;
        }

        return DamageUtil.calculate(crystal.getX(),
                                    crystal.getY(),
                                    crystal.getZ(),
                                    Managers.POSITION.getBB(),
                                    mc.player);
    }

    public float getDamage(Entity crystal, Box bb)
    {
        if (module.isSuicideModule())
        {
            return 0.0f;
        }

        return DamageUtil.calculate(crystal.getX(),
                                    crystal.getY(),
                                    crystal.getZ(),
                                    bb,
                                    RotationUtil.getRotationPlayer());
    }

    // Break damage for other entities //
    public float getDamage(Entity crystal, LivingEntity base)
    {
        return getDamage(crystal.getX(), crystal.getY(), crystal.getZ(), base,
                mc.world, bExtrapolation.getValue(),
                         module.avgBreakExtra.getValue(), false, false,
                         terrainCalc.getValue());
    }

    // Place damage for ourselves //
    public float getDamage(BlockPos pos)
    {
        if (module.isSuicideModule())
        {
            return 0.0f;
        }

        return getDamage(pos, RotationUtil.getRotationPlayer(), mc.world,
                         pExtrapolation.getValue(),
                         module.avgPlaceDamage.getValue(), true,
                         terrainCalc.getValue());
    }

    // Place damage for other entities //
    public float getDamage(BlockPos pos, LivingEntity base)
    {
        return getDamage(pos, base, mc.world,
                         pExtrapolation.getValue(),
                         module.avgPlaceDamage.getValue(), false,
                         terrainCalc.getValue());
    }

    // Obby place damage for ourselves //
    public float getObbyDamage(BlockPos pos,
                               WorldAccess world)
    {
        if (module.isSuicideModule())
        {
            return 0.0f;
        }

        return getDamage(pos, RotationUtil.getRotationPlayer(), world,
                         pExtrapolation.getValue(),
                         module.avgPlaceDamage.getValue(), true,
                         obbyTerrain.getValue());
    }

    //  Obby place damage for other entities  //
    public float getObbyDamage(BlockPos pos,
                               LivingEntity base,
                               WorldAccess world)
    {
        return getDamage(pos, base, world, pExtrapolation.getValue(),
                         module.avgPlaceDamage.getValue(), false,
                         obbyTerrain.getValue());
    }

    // Takes a BlockPos, for placing only //
    private float getDamage(BlockPos pos, LivingEntity base,
                            WorldAccess world, int ticks, boolean avg,
                            boolean self, boolean terrain)
    {
        return getDamage(pos.getX() + 0.5f, pos.getY() + 1, pos.getZ() + 0.5f,
                         base, world, ticks, avg, self, true, terrain);
    }

    // Where it all comes together //
    private float getDamage(double x, double y, double z, LivingEntity base,
                            WorldAccess world, int ticks, boolean avg,
                            boolean self, boolean place, boolean terrain)
    {
        MotionTracker tracker;
        if (ticks == 0
            || self && !selfExtrapolation.getValue()
            || (tracker = place
                ? positionHelper.getTrackerFromEntity(base)
                : positionHelper.getBreakTrackerFromEntity(base)) == null
            || !tracker.active) {
            return DamageUtil.calculate(x, y, z, base.getBoundingBox(),
                                        base, (ClientWorld) world, terrain);
        }

        float dmg = DamageUtil.calculate(x, y, z,
                                         tracker.getBoundingBox(),
                                         base, (ClientWorld) world, terrain);
        if (avg) {
            double extraWeight = place
                ? module.placeExtraWeight.getValue()
                : module.breakExtraWeight.getValue();
            double normWeight  = place
                ? module.placeNormalWeight.getValue()
                : module.breakNormalWeight.getValue();

            float normDmg = DamageUtil.calculate(x, y, z,
                                                 base.getBoundingBox(),
                                                 base, (ClientWorld) world, terrain);
            return (float) ((normDmg * normWeight + dmg * extraWeight) / 2.0);
        }

        return dmg;
    }

}
