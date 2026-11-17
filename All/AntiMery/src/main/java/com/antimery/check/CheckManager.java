package com.antimery.check;

import com.antimery.AntiMery;
import com.antimery.check.chat.ChatFilterCheck;
import com.antimery.check.combat.AutoClickerCheck;
import com.antimery.check.combat.CriticalsCheck;
import com.antimery.check.combat.KillAuraCheck;
import com.antimery.check.combat.ReachCheck;
import com.antimery.check.movement.*;
import com.antimery.check.world.FastBreakCheck;
import com.antimery.check.world.FastPlaceCheck;
import com.antimery.check.world.FastUseCheck;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CheckManager {

    private final AntiMery plugin;
    private final Map<CheckType, Check> checks = new HashMap<>();

    // Specific typed references
    private FlyCheck flyCheck;
    private SpeedCheck speedCheck;
    private NoFallCheck noFallCheck;
    private JesusCheck jesusCheck;
    private HighJumpCheck highJumpCheck;
    private FastClimbCheck fastClimbCheck;
    private PredictionCheck predictionCheck;

    private KillAuraCheck killAuraCheck;
    private ReachCheck reachCheck;
    private AutoClickerCheck autoClickerCheck;
    private CriticalsCheck criticalsCheck;

    private FastBreakCheck fastBreakCheck;
    private FastPlaceCheck fastPlaceCheck;
    private FastUseCheck fastUseCheck;

    private ChatFilterCheck chatFilterCheck;

    public CheckManager(AntiMery plugin) {
        this.plugin = plugin;
        registerChecks();
    }

    public void registerChecks() {
        checks.clear();

        flyCheck = new FlyCheck(plugin);
        speedCheck = new SpeedCheck(plugin);
        noFallCheck = new NoFallCheck(plugin);
        jesusCheck = new JesusCheck(plugin);
        highJumpCheck = new HighJumpCheck(plugin);
        fastClimbCheck = new FastClimbCheck(plugin);
        predictionCheck = new PredictionCheck(plugin);

        killAuraCheck = new KillAuraCheck(plugin);
        reachCheck = new ReachCheck(plugin);
        autoClickerCheck = new AutoClickerCheck(plugin);
        criticalsCheck = new CriticalsCheck(plugin);

        fastBreakCheck = new FastBreakCheck(plugin);
        fastPlaceCheck = new FastPlaceCheck(plugin);
        fastUseCheck = new FastUseCheck(plugin);

        chatFilterCheck = new ChatFilterCheck(plugin);

        register(flyCheck);
        register(speedCheck);
        register(noFallCheck);
        register(jesusCheck);
        register(highJumpCheck);
        register(fastClimbCheck);
        register(predictionCheck);

        register(killAuraCheck);
        register(reachCheck);
        register(autoClickerCheck);
        register(criticalsCheck);

        register(fastBreakCheck);
        register(fastPlaceCheck);
        register(fastUseCheck);

        register(chatFilterCheck);
    }

    private void register(Check check) {
        checks.put(check.getType(), check);
    }

    public void reload() {
        chatFilterCheck.loadWordLists();
    }

    public void handleQuit(Player player) {
        predictionCheck.handleQuit(player);
        flyCheck.handleQuit(player);
        speedCheck.handleQuit(player);
        highJumpCheck.handleQuit(player);
        killAuraCheck.handleQuit(player);
        autoClickerCheck.handleQuit(player);
        fastBreakCheck.handleQuit(player);
        fastPlaceCheck.handleQuit(player);
        fastUseCheck.handleQuit(player);
    }

    public Map<CheckType, Check> getChecks() {
        return checks;
    }

    public FlyCheck getFlyCheck() {
        return flyCheck;
    }

    public SpeedCheck getSpeedCheck() {
        return speedCheck;
    }

    public NoFallCheck getNoFallCheck() {
        return noFallCheck;
    }

    public JesusCheck getJesusCheck() {
        return jesusCheck;
    }

    public HighJumpCheck getHighJumpCheck() {
        return highJumpCheck;
    }

    public FastClimbCheck getFastClimbCheck() {
        return fastClimbCheck;
    }

    public PredictionCheck getPredictionCheck() {
        return predictionCheck;
    }

    public KillAuraCheck getKillAuraCheck() {
        return killAuraCheck;
    }

    public ReachCheck getReachCheck() {
        return reachCheck;
    }

    public AutoClickerCheck getAutoClickerCheck() {
        return autoClickerCheck;
    }

    public CriticalsCheck getCriticalsCheck() {
        return criticalsCheck;
    }

    public FastBreakCheck getFastBreakCheck() {
        return fastBreakCheck;
    }

    public FastPlaceCheck getFastPlaceCheck() {
        return fastPlaceCheck;
    }

    public FastUseCheck getFastUseCheck() {
        return fastUseCheck;
    }

    public ChatFilterCheck getChatFilterCheck() {
        return chatFilterCheck;
    }
}

