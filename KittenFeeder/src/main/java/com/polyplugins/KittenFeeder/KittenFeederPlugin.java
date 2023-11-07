package com.polyplugins.KittenFeeder;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.task.Scheduler;


@PluginDescriptor(
        name = "Kitten Feeder",
        description = "Feeds your kitten every few minutes",
        enabledByDefault = false,
        tags = {"poly", "plugin"}
)
@Slf4j
public class KittenFeederPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KittenFeederConfig config;
    public int timeout = 0;
    private long startTime = 0;
    @Inject
    public ItemManager itemManager;


    @Provides
    private KittenFeederConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(KittenFeederConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        timeout = 0;
        startTime = System.currentTimeMillis();
        EthanApiPlugin.sendClientMessage("Kitten Feeder started");
        EthanApiPlugin.sendClientMessage("ONLY FEEDS EVERY `X` MINUTES - DONT BLAME ME IF YOUR CAT DIES");
    }

    @Override
    protected void shutDown() throws Exception {
        timeout = 0;
    }


    @Subscribe
    private void onGameTick(GameTick event) {

        if (!hasFollower()) {
            EthanApiPlugin.sendClientMessage("NO FOLLOWER, STOPPING");
            EthanApiPlugin.stopPlugin(this);
        }
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        long timeSinceFeed = System.currentTimeMillis() - startTime;
        long feedTime = config.frequency() * 60000L;
        if (timeSinceFeed >= feedTime) {
            NPCs.search().withName("Kitten").interactingWithLocal().first().ifPresent(npc -> {
                Inventory.search().onlyUnnoted().withName(config.food()).first().ifPresentOrElse(item -> {
                    MousePackets.queueClickPacket();
                    MousePackets.queueClickPacket();
                    NPCPackets.queueWidgetOnNPC(npc, item);
                }, () -> {
                    EthanApiPlugin.sendClientMessage(String.format("NO %s FOUND, STOPPING", config.food()));
                    EthanApiPlugin.stopPlugin(this);
                });
            });
            startTime = System.currentTimeMillis();
        }

    }

    private boolean hasFollower() {
        return client.getVarpValue(447) > 0;
    }

}