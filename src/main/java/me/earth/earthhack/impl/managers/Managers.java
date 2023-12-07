package me.earth.earthhack.impl.managers;

import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.plugin.Plugin;
import me.earth.earthhack.impl.Earthhack;
import me.earth.earthhack.impl.managers.chat.ChatManager;
import me.earth.earthhack.impl.managers.chat.CommandManager;
import me.earth.earthhack.impl.managers.client.FileManager;
import me.earth.earthhack.impl.managers.client.ModuleManager;
import me.earth.earthhack.impl.managers.client.PlayerManager;
import me.earth.earthhack.impl.managers.client.PluginManager;
import me.earth.earthhack.impl.managers.client.macro.MacroManager;
import me.earth.earthhack.impl.managers.config.ConfigManager;
import me.earth.earthhack.impl.managers.minecraft.combat.CombatManager;
import me.earth.earthhack.impl.managers.minecraft.combat.TargetManager;
import me.earth.earthhack.impl.managers.minecraft.movement.ActionManager;
import me.earth.earthhack.impl.managers.minecraft.movement.NCPManager;
import me.earth.earthhack.impl.managers.minecraft.movement.PositionManager;
import me.earth.earthhack.impl.managers.minecraft.movement.RotationManager;
import me.earth.earthhack.impl.managers.minecraft.timer.TimerManager;
import me.earth.earthhack.impl.managers.thread.EntityProvider;
import me.earth.earthhack.impl.managers.thread.ThreadManager;
import me.earth.earthhack.impl.managers.thread.lookup.LookUpManager;

import java.io.IOException;

/**
 * The internals of the Client.
 * Loads managers upon startup and acts as a Manager for Managers.
 */
public class Managers
{
    public static final ThreadManager THREAD       = new ThreadManager();
    public static final MacroManager MACRO         = new MacroManager();
    public static final ChatManager CHAT           = new ChatManager();
    public static final PlayerManager FRIENDS      = new PlayerManager();
    public static final PlayerManager ENEMIES      = new PlayerManager();
    public static final CombatManager COMBAT       = new CombatManager();
    public static final ModuleManager MODULES      = new ModuleManager();
    public static final PositionManager POSITION   = new PositionManager();
    public static final RotationManager ROTATION   = new RotationManager();
    public static final ActionManager ACTION       = new ActionManager();
    public static final TimerManager TIMER         = new TimerManager();
    public static final NCPManager NCP             = new NCPManager();
    public static final LookUpManager LOOK_UP      = new LookUpManager();
    public static final ConfigManager CONFIG       = new ConfigManager();
    public static final TargetManager TARGET       = new TargetManager();
    public static final EntityProvider ENTITIES    = new EntityProvider();
    public static final FileManager FILES          = new FileManager();
    public static final CommandManager COMMANDS    = new CommandManager();

    /**
     * Loads all Managers, starts the Event System and loads Plugins.
     */
    public static void load() {
        Earthhack.getLogger().info("Subscribing Managers.");
        Earthhack.getLogger().info("Starting Event System.");
        subscribe(TIMER, CHAT, POSITION, ROTATION, ACTION, FILES, COMBAT, TARGET, ENTITIES);


        Earthhack.getLogger().info("Loading Commands");
        COMMANDS.init();
        subscribe(COMMANDS);
        Earthhack.getLogger().info("Loading Modules");
        MODULES.init();
        /*
         * Initialize PingBypass here!!!
         */
        Earthhack.getLogger().info("Loading Plugins");
        PluginManager.getInstance().instantiatePlugins();
        for (Plugin plugin : PluginManager.getInstance().getPlugins().values())
        {
            plugin.load();
        }
        Earthhack.getLogger().info("Loading Configs");
        try
        {
            CONFIG.refreshAll();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Earthhack.getLogger().info("Initializing Modules");
        MODULES.load();
    }

    public static void subscribe(Object...subscribers)
    {
        for (Object subscriber : subscribers)
        {
            Bus.EVENT_BUS.subscribe(subscriber);
        }
    }
}
