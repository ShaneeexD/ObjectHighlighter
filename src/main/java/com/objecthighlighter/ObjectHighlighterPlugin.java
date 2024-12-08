package com.objecthighlighter;

import com.google.inject.Provides;
import com.objecthighlighter.HighlightedObject.HighlightStyle;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import java.awt.Color;
import javax.swing.JColorChooser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import java.awt.event.KeyEvent;

@Slf4j
@PluginDescriptor(
    name = "Object Highlighter",
    description = "Highlight objects by shift + right-clicking them.",
    enabledByDefault = true
)
public class ObjectHighlighterPlugin extends Plugin implements KeyListener
{
    @Inject
    private Client client;

    @Inject
    private ObjectHighlighterConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ObjectHighlightOverlay overlay;

    @Inject
    private ConfigManager configManager;

    @Inject
    private KeyManager keyManager;

    @Getter
    private final Map<TileObject, HighlightedObject> highlightedObjects = new HashMap<>();
    private boolean shiftPressed = false;
    private static final String HIGHLIGHT = "Highlight";
    private static final String HIGHLIGHT_ALL = "Highlight All";
    private static final String REMOVE_ALL = "Remove Highlight All";
    private static final String REMOVE_HIGHLIGHT = "Remove Highlight";
    private static final String CHOOSE_COLOR = "Choose Color";
    private static final String STYLE_PREFIX = "Style: ";
    private static final String STYLE_OUTLINE = STYLE_PREFIX + "Outline";
    private static final String STYLE_HITBOX = STYLE_PREFIX + "Clickbox";

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(this);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            loadHighlightedObjects();
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(this);
        saveHighlightedObjects();
        highlightedObjects.clear();
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            shiftPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT)
        {
            shiftPressed = false;
        }
    }

    private void loadHighlightedObjects()
    {
        String json = config.highlightedObjects();
        if (json == null || json.isEmpty())
        {
            log.debug("No highlighted objects found in config");
            return;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<SavedHighlight>>(){}.getType();
        List<SavedHighlight> savedHighlights = gson.fromJson(json, type);

        if (savedHighlights != null)
        {
            log.debug("Loading {} saved highlights", savedHighlights.size());
            for (SavedHighlight saved : savedHighlights)
            {
                if (saved.isHighlightAll())
                {
                    // For highlight all, we need to find all matching objects
                    Scene scene = client.getScene();
                    Tile[][][] tiles = scene.getTiles();

                    for (int z = 0; z < tiles.length; z++)
                    {
                        for (int x = 0; x < tiles[z].length; x++)
                        {
                            for (int y = 0; y < tiles[z][x].length; y++)
                            {
                                Tile tile = tiles[z][x][y];
                                if (tile != null)
                                {
                                    TileObject found = findObjectById(tile, saved.getId());
                                    if (found != null)
                                    {
                                        highlightedObjects.put(found, new HighlightedObject(
                                            found,
                                            saved.getColor(),
                                            saved.getStyle()
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    // For single object highlights, try to find the specific object
                    WorldPoint worldPoint = new WorldPoint(saved.getX(), saved.getY(), saved.getPlane());
                    LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
                    
                    if (localPoint != null)
                    {
                        int regionX = localPoint.getSceneX();
                        int regionY = localPoint.getSceneY();
                        
                        if (regionX >= 0 && regionY >= 0)
                        {
                            TileObject obj = findTileObject(saved.getId(), regionX, regionY);
                            if (obj != null)
                            {
                                highlightedObjects.put(obj, new HighlightedObject(
                                    obj,
                                    saved.getColor(),
                                    saved.getStyle()
                                ));
                                log.debug("Loaded object {} at ({}, {})", saved.getId(), regionX, regionY);
                            }
                            else
                            {
                                log.debug("Could not find object {} at ({}, {})", saved.getId(), regionX, regionY);
                            }
                        }
                    }
                    else
                    {
                        log.debug("Could not convert world point to local: {}", worldPoint);
                    }
                }
            }
            log.debug("Loaded {} objects", highlightedObjects.size());
        }
    }

    private void saveHighlightedObjects()
    {
        List<SavedHighlight> toStore = new ArrayList<>();
        for (Map.Entry<TileObject, HighlightedObject> entry : highlightedObjects.entrySet())
        {
            TileObject obj = entry.getKey();
            HighlightedObject highlight = entry.getValue();
            LocalPoint loc = obj.getLocalLocation();
            WorldPoint worldLoc = WorldPoint.fromLocal(client, loc);
            
            if (worldLoc != null)
            {
                boolean isHighlightAll = false;
                // Check if this is part of a highlight all group
                for (Map.Entry<TileObject, HighlightedObject> other : highlightedObjects.entrySet())
                {
                    if (other.getKey() != obj && other.getKey().getId() == obj.getId() &&
                        other.getValue().getColor().equals(highlight.getColor()) &&
                        other.getValue().getStyle() == highlight.getStyle())
                    {
                        isHighlightAll = true;
                        break;
                    }
                }

                toStore.add(SavedHighlight.fromHighlightedObject(
                    highlight,
                    worldLoc.getX(),
                    worldLoc.getY(),
                    worldLoc.getPlane(),
                    isHighlightAll
                ));
            }
        }

        Gson gson = new Gson();
        String json = gson.toJson(toStore);
        configManager.setConfiguration("objecthighlighter", "highlightedObjects", json);
        log.debug("Saved {} highlights", toStore.size());
    }

    private TileObject findObjectById(Tile tile, int id)
    {
        GameObject[] gameObjects = tile.getGameObjects();
        for (GameObject gameObject : gameObjects)
        {
            if (gameObject != null && gameObject.getId() == id)
            {
                return gameObject;
            }
        }

        DecorativeObject decorativeObject = tile.getDecorativeObject();
        if (decorativeObject != null && decorativeObject.getId() == id)
        {
            return decorativeObject;
        }

        WallObject wallObject = tile.getWallObject();
        if (wallObject != null && wallObject.getId() == id)
        {
            return wallObject;
        }

        GroundObject groundObject = tile.getGroundObject();
        if (groundObject != null && groundObject.getId() == id)
        {
            return groundObject;
        }

        return null;
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!shiftPressed)
        {
            return;
        }

        final int type = event.getType();
        if (type >= 1000 && type <= 3000)
        {
            final TileObject tileObject = findTileObject(event.getIdentifier(), event.getActionParam0(), event.getActionParam1());

            if (tileObject == null)
            {
                return;
            }

            // Add highlight/remove option
            client.createMenuEntry(-1)
                .setOption(highlightedObjects.containsKey(tileObject) ? REMOVE_HIGHLIGHT : HIGHLIGHT)
                .setTarget(event.getTarget())
                .setIdentifier(event.getIdentifier())
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setType(MenuAction.RUNELITE)
                .setDeprioritized(true);

            // Add highlight all option for non-highlighted objects
            if (!highlightedObjects.containsKey(tileObject))
            {
                client.createMenuEntry(-1)
                    .setOption(HIGHLIGHT_ALL)
                    .setTarget(event.getTarget())
                    .setIdentifier(event.getIdentifier())
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setType(MenuAction.RUNELITE)
                    .setDeprioritized(true);
            }
            else
            {
                // Add remove all option for highlighted objects
                client.createMenuEntry(-1)
                    .setOption(REMOVE_ALL)
                    .setTarget(event.getTarget())
                    .setIdentifier(event.getIdentifier())
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setType(MenuAction.RUNELITE)
                    .setDeprioritized(true);
            }

            // Add style and color options for highlighted objects
            if (highlightedObjects.containsKey(tileObject))
            {
                client.createMenuEntry(-1)
                    .setOption(CHOOSE_COLOR)
                    .setTarget(event.getTarget())
                    .setIdentifier(event.getIdentifier())
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setType(MenuAction.RUNELITE)
                    .setDeprioritized(true);

                client.createMenuEntry(-1)
                    .setOption(STYLE_OUTLINE)
                    .setTarget(event.getTarget())
                    .setIdentifier(event.getIdentifier())
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setType(MenuAction.RUNELITE)
                    .setDeprioritized(true);

                client.createMenuEntry(-1)
                    .setOption(STYLE_HITBOX)
                    .setTarget(event.getTarget())
                    .setIdentifier(event.getIdentifier())
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setType(MenuAction.RUNELITE)
                    .setDeprioritized(true);
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGGED_IN)
        {
            highlightedObjects.clear();
            loadHighlightedObjects();
        }
        else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
        {
            highlightedObjects.clear();
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getMenuAction() != MenuAction.RUNELITE)
        {
            return;
        }

        final TileObject tileObject = findTileObject(event.getId(), event.getActionParam(), event.getWidgetId());
        if (tileObject == null)
        {
            return;
        }

        switch (event.getMenuOption())
        {
            case HIGHLIGHT:
                highlightObject(tileObject, false);
                saveHighlightedObjects();
                break;

            case HIGHLIGHT_ALL:
                highlightObject(tileObject, true);
                saveHighlightedObjects();
                break;

            case REMOVE_HIGHLIGHT:
                highlightedObjects.remove(tileObject);
                saveHighlightedObjects();
                break;

            case REMOVE_ALL:
                // Remove all objects with the same ID
                int objectId = tileObject.getId();
                highlightedObjects.entrySet().removeIf(entry -> entry.getKey().getId() == objectId);
                saveHighlightedObjects();
                break;

            case CHOOSE_COLOR:
                chooseColor(tileObject);
                break;

            case STYLE_OUTLINE:
                setStyle(tileObject, HighlightStyle.OUTLINE);
                break;

            case STYLE_HITBOX:
                setStyle(tileObject, HighlightStyle.HITBOX);
                break;
        }
    }

    private void highlightObject(TileObject tileObject, boolean highlightAll)
    {
        final Color color = config.defaultColor();
        // Apply 50% transparency
        final Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
        
        if (highlightAll)
        {
            Scene scene = client.getScene();
            Tile[][][] tiles = scene.getTiles();
            int z = client.getPlane();
            for (int x = 0; x < Constants.SCENE_SIZE; x++)
            {
                for (int y = 0; y < Constants.SCENE_SIZE; y++)
                {
                    Tile tile = tiles[z][x][y];
                    if (tile != null)
                    {
                        TileObject found = findObjectById(tile, tileObject.getId());
                        if (found != null && found != tileObject)
                        {
                            highlightedObjects.put(found, new HighlightedObject(found, transparentColor, HighlightStyle.OUTLINE));
                        }
                    }
                }
            }
        }

        highlightedObjects.put(tileObject, new HighlightedObject(tileObject, transparentColor, HighlightStyle.OUTLINE));
        saveHighlightedObjects();
    }

    private void setStyle(TileObject tileObject, HighlightStyle style)
    {
        // Get all objects with the same ID
        int objectId = tileObject.getId();
        for (Map.Entry<TileObject, HighlightedObject> entry : highlightedObjects.entrySet())
        {
            if (entry.getKey().getId() == objectId)
            {
                entry.getValue().setStyle(style);
            }
        }
        saveHighlightedObjects();
    }

    private void chooseColor(TileObject tileObject)
    {
        HighlightedObject obj = highlightedObjects.get(tileObject);
        if (obj != null)
        {
            Color currentColor = obj.getColor();
            Color newColor = JColorChooser.showDialog(null, "Choose a color", currentColor);
            if (newColor != null)
            {
                obj.setColor(newColor);
                saveHighlightedObjects();
            }
        }
    }

    private TileObject findTileObject(int id, int x, int y)
    {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = client.getPlane();

        // Add bounds checking
        if (z < 0 || z >= tiles.length || x < 0 || x >= tiles[z].length || y < 0 || y >= tiles[z][x].length)
        {
            return null;
        }

        Tile tile = tiles[z][x][y];
        if (tile == null)
        {
            return null;
        }

        return findObjectById(tile, id);
    }

    @Provides
    ObjectHighlighterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ObjectHighlighterConfig.class);
    }
}
