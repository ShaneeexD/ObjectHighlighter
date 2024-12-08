package com.objecthighlighter;

import lombok.Getter;
import lombok.Setter;
import java.awt.Color;
import net.runelite.api.TileObject;

public class HighlightedObject {
    public enum HighlightStyle {
        OUTLINE,
        HITBOX
    }

    @Getter
    private final TileObject tileObject;
    
    @Getter @Setter
    private Color color;
    
    @Getter @Setter
    private HighlightStyle style;
    
    @Getter
    private int x;
    
    @Getter
    private int y;
    
    @Getter
    private int plane;

    @Getter @Setter
    private boolean highlightAll;

    public HighlightedObject(TileObject tileObject, Color color, HighlightStyle style) {
        this.tileObject = tileObject;
        this.color = color;
        this.style = style;
        this.highlightAll = false;
    }

    public HighlightedObject(int id, Color color, HighlightStyle style, int x, int y, int plane) {
        this.tileObject = null;
        this.color = color;
        this.style = style;
        this.x = x;
        this.y = y;
        this.plane = plane;
        this.highlightAll = false;
    }

    public int getId() {
        return tileObject != null ? tileObject.getId() : -1;
    }
}
