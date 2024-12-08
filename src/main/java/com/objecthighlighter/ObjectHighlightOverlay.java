package com.objecthighlighter;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

public class ObjectHighlightOverlay extends Overlay
{
    private final Client client;
    private final ObjectHighlighterPlugin plugin;
    private final ObjectHighlighterConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    private ObjectHighlightOverlay(Client client, ObjectHighlighterPlugin plugin, ObjectHighlighterConfig config, ModelOutlineRenderer modelOutlineRenderer)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        for (Map.Entry<TileObject, HighlightedObject> entry : plugin.getHighlightedObjects().entrySet())
        {
            TileObject object = entry.getKey();
            HighlightedObject highlightedObject = entry.getValue();

            if (object.getPlane() != client.getPlane())
            {
                continue;
            }

            renderTileObject(graphics, object, highlightedObject);
        }

        return null;
    }

    private void renderTileObject(Graphics2D graphics, TileObject tileObject, HighlightedObject highlightedObject)
    {
        Color color = highlightedObject.getColor();
        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), config.fillAlpha());
        Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);

        switch (highlightedObject.getStyle())
        {
            case OUTLINE:
                modelOutlineRenderer.drawOutline(tileObject, (int)config.borderWidth(), outlineColor, outlineColor.getAlpha());
                break;
            case HITBOX:
                Shape clickbox = tileObject.getClickbox();
                if (clickbox != null)
                {
                    graphics.setColor(fillColor);
                    graphics.fill(clickbox);
                    graphics.setColor(outlineColor);
                    graphics.setStroke(new BasicStroke((float)config.borderWidth()));
                    graphics.draw(clickbox);
                }
                break;
        }

        if (config.debugObjects())
        {
            LocalPoint lp = tileObject.getLocalLocation();
            Point point = Perspective.localToCanvas(client, lp, client.getPlane());
            if (point != null)
            {
                String objectName = "";
                if (tileObject instanceof GameObject)
                {
                    ObjectComposition def = client.getObjectDefinition(tileObject.getId());
                    if (def != null)
                    {
                        objectName = def.getName();
                    }
                }
                WorldPoint worldPoint = WorldPoint.fromLocal(client, lp);
                String debugText = String.format("%s (ID: %d)%nWorld: %d, %d, %d", 
                    objectName, tileObject.getId(),
                    worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());

                Point textLocation = new Point(point.getX(), point.getY() - 40);
                renderDebugText(graphics, debugText, textLocation);
            }
        }
    }

    private void renderDebugText(Graphics2D graphics, String text, Point position)
    {
        Font originalFont = graphics.getFont();
        graphics.setFont(new Font("Arial", Font.BOLD, 12));

        // Draw text background
        String[] lines = text.split("\n");
        FontMetrics fm = graphics.getFontMetrics();
        int maxWidth = 0;
        int totalHeight = fm.getHeight() * lines.length;
        
        for (String line : lines)
        {
            maxWidth = Math.max(maxWidth, fm.stringWidth(line));
        }

        Rectangle bounds = new Rectangle(
            position.getX() - maxWidth / 2 - 2,
            position.getY() - totalHeight,
            maxWidth + 4,
            totalHeight + 2
        );

        graphics.setColor(new Color(0, 0, 0, 128));
        graphics.fill(bounds);

        // Draw text
        graphics.setColor(Color.WHITE);
        int y = position.getY() - totalHeight + fm.getAscent();
        for (String line : lines)
        {
            int x = position.getX() - fm.stringWidth(line) / 2;
            graphics.drawString(line, x, y);
            y += fm.getHeight();
        }

        graphics.setFont(originalFont);
    }
}
