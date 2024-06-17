package com.ef9.EfKebabTownKilla;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.Collections.query.NPCQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.NPCInteraction;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.NPCPackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.util.HotkeyListener;

import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
public class KebabTownKillaPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KebabTownKillaConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ReflectBreakHandler breakHandler;
    private com.ef9.EfKebabTownKilla.State state;
    private boolean started;

    private boolean startedHealing = false;
    private int timeout;

    private static final Logger log = LoggerFactory.getLogger(KebabTownKillaPlugin.class);

    private WorldPoint CabbagesWorldPoint = new WorldPoint(3052, 3504, 0);
    private WorldPoint MonksWorldPoint = new WorldPoint(3052, 3490, 0);


    private WorldPoint KebabManWorldPoint = new WorldPoint(3274, 3180, 0);
    private WorldPoint CenterRoomWorldPoint = new WorldPoint(3292, 3172, 0);
    private WorldPoint EastRoomWorldPoint = new WorldPoint(3284, 3172, 0);
    private WorldPoint WestRoomWorldPoint = new WorldPoint(3301, 3172, 0);

    private WorldArea KebabManWorldArea = new WorldArea(KebabManWorldPoint, 6, 6);
    private WorldArea CenterRoomWorldArea = new WorldArea(CenterRoomWorldPoint, 12, 12);
    private WorldArea EastRoomWorldArea = new WorldArea(EastRoomWorldPoint, 5, 11);
    private WorldArea WestRoomWorldArea = new WorldArea(WestRoomWorldPoint, 5, 11);

    private WorldArea CabbagesWorldArea = new WorldArea(CabbagesWorldPoint, 5, 5);
    private WorldArea MonksWorldArea = new WorldArea(MonksWorldPoint, 16, 16);

    @Getter
    @Setter
    private List<String> foodNames = List.of("Kebab", "kebab");

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
    private KebabTownKillaConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(KebabTownKillaConfig.class);
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
        if(!config.stfu()){
            System.out.println(state.name());
        }
        switch (state) {
            case RUNNING_MOBS:
                runToCenterRoom();
                break;
            case RUNNING_KEBABMAN:
                runToKebabMan();
                break;
            case TALK_KEBABMAN:
                talkToKebabMan();
                break;
            case CONTINUE:
                continueAtKebabMan();
                break;
            case GET_KEBAB:
                getHealAtMonk();
                break;
            case ATTACK_MOB:
                attackMob();
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
        if (breakHandler.shouldBreak(this) && isAtKebabMan()) {
            return State.BREAK;
        }

        if (timeout > 0 && state != State.WAITING) {
            timeout--;
            return State.TIMEOUT;
        }

        if(isHealingEnabled() && !isInCombat() && (isAtCenterRoom() || isAtEastRoom() || isAtWestRoom()) && needToHeal() && !startedHealing){
            startedHealing = true;
        }

        if(breakHandler.shouldBreak(this) || (isHealingEnabled() && isInCombat() && needToHeal() && (isAtCenterRoom() || isAtEastRoom() || isAtWestRoom()) && !startedHealing)){
            return State.RUNNING_KEBABMAN;
        }

        if(isAtKebabMan() || !isInCombat()){
            return State.RUNNING_MOBS;
        }

        if(!isInCombat() && isAtKebabMan() && (needToHeal() || startedHealing) && !hasContinueWidget() && !hasHealWidget() && !stopHealing()){
            return State.TALK_KEBABMAN;
        }

        if(startedHealing && hasContinueWidget()){
            return State.CONTINUE;
        }

        if(hasGetKebabWidget()){
            return State.GET_KEBAB;
        }

        if((isAtCenterRoom() || isAtEastRoom() || isAtWestRoom()) && !isInCombat() && !needToHeal() && !startedHealing){
            return State.ATTACK_MOB;
        }

        if(isAtMonks() && isInCombat() && !needToHeal()){
            return State.ATTACKING_MOB;
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

    private boolean hasGetKebabWidget(){
        return !Widgets.search().withTextContains("Yes please").hiddenState(false).empty();
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
    private boolean isAtKebabMan(){
        return KebabManWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }
    private boolean isAtCenterRoom(){
        return CenterRoomWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }
    private boolean isAtEastRoom(){
        return EastRoomWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }
    private boolean isAtWestRoom(){
        return WestRoomWorldArea.contains(client.getLocalPlayer().getWorldLocation()); }

    private void talkToMonk(){
        List<NPC> MonksInArea = NPCs.search().withName("Monk").notInteracting().withinWorldArea(MonksWorldArea).result();
        if (!MonksInArea.isEmpty()) {
            MonksInArea = MonksInArea.stream().filter(x -> !x.isInteracting()).collect(Collectors.toList());
            NPCInteraction.interact(MonksInArea.get(0), "Talk-to");
            timeout = 5;
            // do more heal shit later
        }
    }
    private void talkToKebabMan(){
        List<NPC> KharimInArea = NPCs.search().withName("Karim").notInteracting().withinWorldArea(KebabManWorldArea).result();
        if (!KharimInArea.isEmpty()) {
            KharimInArea = KharimInArea.stream().filter(x -> !x.isInteracting()).collect(Collectors.toList());
            NPCInteraction.interact(KharimInArea.get(0), "Talk-to");
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

    private void continueAtKebabMan(){
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

    private void getKebabAtKebabMan(){
        Widgets.search().withTextContains("Yes please").hiddenState(false).first().ifPresent(w -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(w.getId(), 1);
            timeout = 5;
        });
    }

    private void attackMob(){
        NPCQuery npc = NPCs.search().alive().walkable().filter(n -> n.getName() != null && targetNames().contains(n.getName())).withAction("Attack").filter(
                n -> !n.isInteracting() || (n.isInteracting() && n.getInteracting() instanceof Player
                        && n.getInteracting().equals(client.getLocalPlayer()))
        );

        NPC MobNPC = npc.nearestToPlayer().orElse(null);

        if (MobNPC != null) {
            if(!config.stfu()){
                log.info("Should fight, found mob");
            }
            MousePackets.queueClickPacket();
            NPCPackets.queueNPCAction(MobNPC, "Attack");
            timeout = 6;
        }
    }

    public List<String> targetNames() {
        return Arrays.asList("Al Kharid warrior");
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
    private void runToKebabMan(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(KebabManWorldPoint);
        timeout = 5;
    }
    private void runToCenterRoom(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(CenterRoomWorldPoint);
        timeout = 5;
    }
    private void runToEastRoom(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(EastRoomWorldPoint);
        timeout = 5;
    }
    private void runToWestRoom(){
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(WestRoomWorldPoint);
        timeout = 5;
    }

    public Widget findFood() {
        Optional<Widget> food = Inventory.search().withAction("Eat").filter(f -> {
            String name = f.getName();
            return foodNames.stream().anyMatch(name::contains);
        }).first();
        return food.orElse(null);
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
