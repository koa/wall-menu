package ch.bergturbenthal.home.service.impl;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ch.bergturbenthal.home.service.Display;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;

public class TableTextDisplayRenderer implements Display.DisplayRenderer {
    @Value
    @Builder
    public static class DisplayText {
        public static DisplayText boldText(final String text) {
            return DisplayText.builder().text(text).style(Font.BOLD).build();
        }

        public static DisplayText plainText(final String text) {
            return DisplayText.builder().text(text).build();
        }

        private String text;
        private int    style;
    }

    @Value
    @Builder
    public static class LeftRightTextRow implements TextRow {
        private DisplayText leftBeforeAlign;
        private DisplayText leftAfterAlign;
        private DisplayText rightBeforeAlign;
        private DisplayText rightAfterAlign;
    }

    public static interface TextRenderer {
        List<TextRow> renderText(int maxRowCount);
    }

    public static interface TextRow {

    }

    @Setter
    private int                fontSize = 10;
    @Setter
    private String             fontName = null;
    private final TextRenderer textRenderer;

    public TableTextDisplayRenderer(final TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    @Override
    public void render(final Graphics2D graphics, final int width, final int height) {
        final Map<Integer, Font> fonts = new HashMap<>();
        final Function<Integer, Font> getFont = style -> fonts.computeIfAbsent(style, k -> new Font(fontName, k, fontSize));
        final Map<Integer, FontMetrics> fontMetrics = new HashMap<>();
        final Function<Integer, FontMetrics> getFontMetrics = style -> fontMetrics.computeIfAbsent(style,
                k -> graphics.getFontMetrics(getFont.apply(k)));
        final FontMetrics plainFontMetrics = getFontMetrics.apply(Font.PLAIN);
        final int minLineHeight = plainFontMetrics.getHeight();
        final int lineCount = Math.max(height / minLineHeight, 1);
        final int lineHeight;
        final int firstRowPos;
        if (lineCount > 1) {
            firstRowPos = plainFontMetrics.getAscent();
            final int lineSpaceCount = lineCount - 1;
            final int blankSpace = height
                    - (plainFontMetrics.getAscent() + plainFontMetrics.getDescent() + lineSpaceCount * plainFontMetrics.getHeight());
            lineHeight = plainFontMetrics.getHeight() + blankSpace / lineSpaceCount;
        } else {
            firstRowPos = plainFontMetrics.getAscent() + height - (plainFontMetrics.getAscent() + plainFontMetrics.getDescent()) / 2;
            lineHeight = 0;
        }
        final List<TextRow> lines = textRenderer.renderText(lineCount);
        if (lines == null) {
            return;
        }

        final Function<DisplayText, Integer> stringWithFunction = text -> getFontMetrics.apply(text.getStyle()).stringWidth(text.getText());

        // calculate spaces
        int leftMaxSpace = 0;
        int rightMaxSpace = 0;
        for (final TextRow textRow : lines) {
            if (textRow instanceof LeftRightTextRow) {
                final LeftRightTextRow leftRightTextRow = (LeftRightTextRow) textRow;
                final DisplayText leftBeforeAlign = leftRightTextRow.getLeftBeforeAlign();
                if (leftBeforeAlign != null) {
                    final int stringWidth = stringWithFunction.apply(leftBeforeAlign);
                    if (stringWidth > leftMaxSpace) {
                        leftMaxSpace = stringWidth;
                    }
                }
                final DisplayText rightAfterAlign = leftRightTextRow.getRightAfterAlign();
                if (rightAfterAlign != null) {
                    final int stringWidth = stringWithFunction.apply(rightAfterAlign);
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
                final DisplayText leftBeforeAlign = leftRightTextRow.getLeftBeforeAlign();
                if (leftBeforeAlign != null) {
                    final int stringWidth = stringWithFunction.apply(leftBeforeAlign);
                    graphics.setFont(getFont.apply(leftBeforeAlign.getStyle()));
                    graphics.drawString(leftBeforeAlign.getText(), leftMaxSpace - stringWidth, y);
                }
                final DisplayText leftAfterAlign = leftRightTextRow.getLeftAfterAlign();
                if (leftAfterAlign != null) {
                    graphics.setFont(getFont.apply(leftAfterAlign.getStyle()));
                    graphics.drawString(leftAfterAlign.getText(), leftMaxSpace, y);
                }
                final DisplayText rightBeforeAlign = leftRightTextRow.getRightBeforeAlign();
                if (rightBeforeAlign != null) {
                    final int stringWidth = stringWithFunction.apply(rightBeforeAlign);
                    graphics.setFont(getFont.apply(rightBeforeAlign.getStyle()));
                    graphics.drawString(rightBeforeAlign.getText(), width - rightMaxSpace - stringWidth, y);
                }
                final DisplayText rightAfterAlign = leftRightTextRow.getRightAfterAlign();
                if (rightAfterAlign != null) {
                    graphics.setFont(getFont.apply(rightAfterAlign.getStyle()));
                    graphics.drawString(rightAfterAlign.getText(), width - rightMaxSpace, y);
                }
            }
        }

    }

}
