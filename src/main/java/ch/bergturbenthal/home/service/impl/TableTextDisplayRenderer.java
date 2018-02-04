package ch.bergturbenthal.home.service.impl;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

import ch.bergturbenthal.home.service.Display;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;

public class TableTextDisplayRenderer implements Display.DisplayRenderer {

    @Value
    @Builder
    public static class LeftRightTextRow implements TextRow {
        private String leftBeforeAlign;
        private String leftAfterAlign;
        private String rightBeforeAlign;
        private String rightAfterAlign;
    }

    public static interface TextRenderer {
        List<TextRow> renderText(int maxRowCount);
    }

    public static interface TextRow {

    }

    @Setter
    private int                fontSize = 10;
    private final Font         baseFont = new Font(Font.SANS_SERIF, 0, 10);
    private final TextRenderer textRenderer;

    public TableTextDisplayRenderer(final TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    @Override
    public void render(final Graphics2D graphics, final int width, final int height) {
        final Font displayFont = baseFont.deriveFont(baseFont.getStyle(), fontSize);
        graphics.setFont(displayFont);
        final FontMetrics fontMetrics = graphics.getFontMetrics();
        final int minLineHeight = fontMetrics.getHeight();
        final int lineCount = Math.max(height / minLineHeight, 1);
        final int lineHeight;
        final int firstRowPos;
        if (lineCount > 1) {
            firstRowPos = fontMetrics.getAscent();
            final int lineSpaceCount = lineCount - 1;
            final int blankSpace = height - (fontMetrics.getAscent() + fontMetrics.getDescent() + lineSpaceCount * fontMetrics.getHeight());
            lineHeight = fontMetrics.getHeight() + blankSpace / lineSpaceCount;
        } else {
            firstRowPos = fontMetrics.getAscent() + height - (fontMetrics.getAscent() + fontMetrics.getDescent()) / 2;
            lineHeight = 0;
        }
        final List<TextRow> lines = textRenderer.renderText(lineCount);
        if (lines == null) {
            return;
        }

        // calculate spaces
        int leftMaxSpace = 0;
        int rightMaxSpace = 0;
        for (final TextRow textRow : lines) {
            if (textRow instanceof LeftRightTextRow) {
                final LeftRightTextRow leftRightTextRow = (LeftRightTextRow) textRow;
                final String leftBeforeAlign = leftRightTextRow.getLeftBeforeAlign();
                if (leftBeforeAlign != null) {
                    final int stringWidth = fontMetrics.stringWidth(leftBeforeAlign);
                    if (stringWidth > leftMaxSpace) {
                        leftMaxSpace = stringWidth;
                    }
                }
                final String rightAfterAlign = leftRightTextRow.getRightAfterAlign();
                if (rightAfterAlign != null) {
                    final int stringWidth = fontMetrics.stringWidth(rightAfterAlign);
                    if (stringWidth > rightMaxSpace) {
                        rightMaxSpace = stringWidth;
                    }
                }
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            final TextRow textRow = lines.get(i);
            final int y = firstRowPos + i * lineHeight;

            if (textRow instanceof LeftRightTextRow) {
                final LeftRightTextRow leftRightTextRow = (LeftRightTextRow) textRow;
                final String leftBeforeAlign = leftRightTextRow.getLeftBeforeAlign();
                if (leftBeforeAlign != null) {
                    final int stringWidth = fontMetrics.stringWidth(leftBeforeAlign);
                    graphics.drawString(leftBeforeAlign, leftMaxSpace - stringWidth, y);
                }
                final String leftAfterAlign = leftRightTextRow.getLeftAfterAlign();
                if (leftAfterAlign != null) {
                    graphics.drawString(leftAfterAlign, leftMaxSpace, y);
                }
                final String rightBeforeAlign = leftRightTextRow.getRightBeforeAlign();
                if (rightBeforeAlign != null) {
                    final int stringWidth = fontMetrics.stringWidth(rightBeforeAlign);
                    graphics.drawString(rightBeforeAlign, width - rightMaxSpace - stringWidth, y);
                }
                final String rightAfterAlign = leftRightTextRow.getRightAfterAlign();
                if (rightAfterAlign != null) {
                    graphics.drawString(rightAfterAlign, width - rightMaxSpace, y);
                }
            }
        }

    }

}
