package com.objecthighlighter;

import java.awt.Color;

import com.objecthighlighter.HighlightedObject.HighlightStyle;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SavedHighlight {
    private int id;
    private int colorRGB;
    private HighlightStyle style;
    private int x;
    private int y;
    private int plane;
    private boolean highlightAll;

    public static SavedHighlight fromHighlightedObject(HighlightedObject obj, int worldX, int worldY, int plane, boolean highlightAll) {
        return new SavedHighlight(
            obj.getId(),
            obj.getColor().getRGB(),
            obj.getStyle(),
            worldX,
            worldY,
            plane,
            highlightAll
        );
    }

    public Color getColor() {
        return new Color(colorRGB, true);
    }
}
