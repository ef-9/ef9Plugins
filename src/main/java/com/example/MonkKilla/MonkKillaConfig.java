package com.example.MonkKilla;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("MonkKilla")
public interface MonkKillaConfig extends Config {
    @ConfigItem(
            keyName = "toggle",
            name = "Toggle",
            description = "",
            position = -2
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            name = "Heal HP",
            keyName = "heal",
            description = "Set 0 to disable healing or it will heal at config HP",
            position = 0
    )
    default int heal() {
        return 0;
    }
}