package com.ef9.EfMonkKilla;

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

    @ConfigItem(
            name = "Strength pot?",
            keyName = "strpot",
            description = "Set 0 to disable healing or it will heal at config HP",
            position = 0
    )
    default boolean strpot() {
        return false;
    }

    @ConfigItem(
            name = "STFU",
            keyName = "silent",
            description = "Turn on for console spam",
            position = 0
    )
    default boolean stfu() {
        return true;
    }
}