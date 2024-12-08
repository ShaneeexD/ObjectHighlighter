package com.objecthighlighter;

import java.awt.Color;

import com.objecthighlighter.HighlightedObject.HighlightStyle;

public class StoredObject {
    int id;
    int color;
    HighlightStyle style;
    int x;
    int y;
    int plane;

    public StoredObject(int id, Color color, HighlightStyle style, int x, int y, int plane) {
        this.id = id;
        this.color = color.getRGB();
        this.style = style;
        this.x = x;
        this.y = y;
        this.plane = plane;
    }
}
