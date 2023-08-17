package com.example.MonkKilla;

import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.util.HotkeyListener;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@PluginDescriptor(
        name = "efMonk Killa",
        description = "Kills Monks, re-aggros and heals if you want it to",
        tags = {"ef9", "monk", "defence"}
)
public class MonkKillaPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private MonkKillaConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ReflectBreakHandler breakHandler;
    private com.example.MonkKilla.State state;
    private boolean started;

    private boolean startedHealing = false;
    private int timeout;

    private static final Logger log = LoggerFactory.getLogger(MonkKillaPlugin.class);

    private WorldPoint CabbagesWorldPoint = new WorldPoint(3052, 3504, 0);
    private WorldPoint MonksWorldPoint = new WorldPoint(3052, 3490, 0);

    private WorldArea CabbagesWorldArea = new WorldArea(CabbagesWorldPoint, 5, 5);
    private WorldArea MonksWorldArea = new WorldArea(MonksWorldPoint, 16, 16);

    private boolean onBreak = false;
    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        breakHandler.registerPlugin(this);
    }

    @Override
    protected void shutDown() throws Exception {
        breakHandler.unregisterPlugin(this);
        keyManager.unregisterKeyListener(toggle);
    }


    @Provides
    private MonkKillaConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(MonkKillaConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (breakHandler.isBreakActive(this) || (!EthanApiPlugin.loggedIn() && !breakHandler.isBreakActive(this)) || !started) {
            return;
        }

        state = getNextState();
        handleState();
    }

    private int tickDelay() {
        return ThreadLocalRandom.current().nextInt(0, 3);
    }

    private void handleState() {
        System.out.println(state.name());
        switch (state) {
            case RUNNING_MONKS:
                runToMonks();
                break;
            case RUNNING_CABBAGES:
                runToCabbages();
                break;
            case TALK_MONK:
                talkToMonk();
                break;
            case CONTINUE:
                continueHealAtMonk();
                break;
            case GET_HEAL:
                getHealAtMonk();
                break;
            case ATTACK_MONK:
                attackMonk();
                break;
            case TIMEOUT:
                timeout--;
                break;
            case BREAK:
                breakHandler.startBreak(this);
                timeout = 10;
                break;
        }
    }


    private State getNextState() {
        if (breakHandler.shouldBreak(this) && isAtCabbages()) {
            return State.BREAK;
        }

/*        if(isAtCabbages() && false){
            return State.BREAK;
        }*/
        if (timeout > 0 && state != State.WAITING) {
            timeout--;
            return State.TIMEOUT;
        }

        if(isHealingEnabled() && !isInCombat() && isAtMonks() && needToHeal() && !startedHealing){
            startedHealing = true;
        }

        if(breakHandler.shouldBreak(this) || (isHealingEnabled() && isInCombat() && needToHeal() && isAtMonks() && !startedHealing)){
            return State.RUNNING_CABBAGES;
        }

        if(isAtCabbages() || (!isAtMonks() && !isInCombat())){
            return State.RUNNING_MONKS;
        }

        if(!isInCombat() && isAtMonks() && (needToHeal() || startedHealing) && !hasContinueWidget() && !hasHealWidget() && !stopHealing()){
            return State.TALK_MONK;
        }

        if(startedHealing && hasContinueWidget()){
            return State.CONTINUE;
        }

        if(startedHealing && hasHealWidget()){
            return State.GET_HEAL;
        }

        if(isAtMonks() && !isInCombat() && !needToHeal() && !startedHealing){
            return State.ATTACK_MONK;
        }

        if(isAtMonks() && isInCombat() && !needToHeal()){
            return State.ATTACKING_MONK;
        }

        if(stopHealing()){
            startedHealing = false;
        }

        return State.WAITING;
    }

    private boolean isUsingStrengthPots(){
        return config.strpot();
    }

    private boolean hasContinueWidget(){
        return !Widgets.search().withTextContains("Click here to continue").hiddenState(false).empty();
    }

    private boolean hasHealWidget(){
        return !Widgets.search().withTextContains("heal me").hiddenState(false).empty();
    }

    private boolean isHealingEnabled(){
        return config.heal() > 0;
    }
    private boolean needToHeal(){
        return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.heal(); }
    public boolean stopHealing(){
        return client.getBoostedSkillLevel(Skill.HITPOINTS) >= client.getRealSkillLevel(Skill.HITPOINTS);
    }
    private boolean isInCombat(){
        Player localPlayer = client.getLocalPlayer();
        return localPlayer.isInteracting() || localPlayer.getAnimation()!=-1 || client.getNpcs().stream().anyMatch(scannedNPC-> {
            if (scannedNPC.getInteracting()!=null) {
                return scannedNPC.getInteracting().equals(localPlayer);
            }
            return false;
        });

    }
    private boolean isAtMonks(){
        return MonksWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }
    private boolean isAtCabbages(){
        return CabbagesWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }

    private void talkToMonk(){
        List<NPC> MonksInArea = NPCs.search().withName("Monk").notInteracting().withinWorldArea(MonksWorldArea).result();
        if (!MonksInArea.isEmpty()) {
            MonksInArea = MonksInArea.stream().filter(x -> !x.isInteracting()).collect(Collectors.toList());
            NPCInteraction.interact(MonksInArea.get(0), "Talk-to");
            timeout = 5;
            // do more heal shit later
        }
    }

    private void continueHealAtMonk(){
        Widgets.search().withTextContains("Click here to continue").hiddenState(false).first().ifPresent(w -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(w.getId(), -1);
            timeout = 5;
        });
    }

    private void getHealAtMonk(){
        Widgets.search().withTextContains("heal me").hiddenState(false).first().ifPresent(w -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(w.getId(), 1);
            timeout = 5;
        });
    }
    private void attackMonk(){
        List<NPC> MonksInArea = NPCs.search().notInteracting().withName("Monk").withinWorldArea(MonksWorldArea).result();
        if (!MonksInArea.isEmpty()) {
            NPCInteraction.interact(MonksInArea.get(0), "Attack");
            timeout = 5;
        }
    }

    private void runToMonks(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(MonksWorldPoint);
        timeout = 5;
    }
    private void runToCabbages(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(CabbagesWorldPoint);
        timeout = 5;
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;
        if (!started) {
            breakHandler.stopPlugin(this);
        } else {
            breakHandler.startPlugin(this);
        }
    }

}
