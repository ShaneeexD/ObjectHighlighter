package com.objecthighlighter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ObjectHighlighterPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ObjectHighlighterPlugin.class);
        RuneLite.main(args);
    }
}