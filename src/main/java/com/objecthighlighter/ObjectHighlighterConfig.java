package com.objecthighlighter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.Color;

@ConfigGroup("objecthighlighter")
public interface ObjectHighlighterConfig extends Config
{
    @ConfigItem(
        keyName = "defaultColor",
        name = "Default highlight color",
        description = "Color of object highlighting",
        position = 1
    )
    default Color defaultColor()
    {
        return new Color(0xFF, 0x0, 0x0, 0xFF);  // Opaque red
    }

    @ConfigItem(
        keyName = "borderWidth",
        name = "Border Width",
        description = "Width of the highlighted object border"
    )
    default double borderWidth()
    {
        return 3;
    }

    @ConfigItem(
        keyName = "fillAlpha",
        name = "Fill Opacity",
        description = "Opacity of the clickbox fill color (0-255)"
    )
    default int fillAlpha()
    {
        return 50;
    }

    @ConfigItem(
        keyName = "highlightedObjects",
        name = "",
        description = "",
        hidden = true
    )
    default String highlightedObjects()
    {
        return "";
    }

    @ConfigItem(
        keyName = "highlightedObjects",
        name = "",
        description = ""
    )
    void setHighlightedObjects(String objects);

    @ConfigItem(
        keyName = "debugObjects",
        name = "Debug Objects",
        description = "Show object name, ID and position above highlighted objects"
    )
    default boolean debugObjects()
    {
        return false;
    }
}
