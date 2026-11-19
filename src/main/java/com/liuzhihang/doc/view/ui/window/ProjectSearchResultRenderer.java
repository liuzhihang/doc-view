package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.intellij.ui.SimpleTextAttributes.*;

/** Computes bounded display fragments off the EDT. Painting only applies the list's current theme. */
final class ProjectSearchResultRenderer implements ListCellRenderer<ProjectSearchResultRenderer.Row> {
    private static final int SNIPPET = 320;
    private static final SimpleTextAttributes SMALL = new SimpleTextAttributes(STYLE_SMALLER, null);
    private static final SimpleTextAttributes MATCH = new SimpleTextAttributes(STYLE_SEARCH_MATCH | STYLE_BOLD, null);
    private static final SimpleTextAttributes SELECTED_MATCH = new SimpleTextAttributes(STYLE_BOLD | STYLE_UNDERLINE, null);
    private final DefaultListCellRenderer defaults = new DefaultListCellRenderer();
    private final JPanel panel = new JPanel();
    private final SimpleColoredComponent[] labels = new SimpleColoredComponent[4];

    ProjectSearchResultRenderer() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < labels.length; i++) {
            var label = new SimpleColoredComponent();
            label.setOpaque(false);
            label.setIpad(JBUI.insets(1, 5));
            panel.add(label);
            labels[i] = label;
        }
    }

    record Row(EntryData entry, List<List<Fragment>> lines, String accessibleText, String tooltip) {
        @Override public String toString() { return accessibleText; }
    }

    private record Fragment(String text, SimpleTextAttributes style) {}

    @Override
    public Component getListCellRendererComponent(JList<? extends Row> list, Row row, int index,
                                                  boolean selected, boolean focus) {
        defaults.getListCellRendererComponent(list, "", index, selected, focus);
        panel.setBackground(defaults.getBackground());
        panel.setForeground(defaults.getForeground());
        panel.setBorder(defaults.getBorder());
        panel.getAccessibleContext().setAccessibleName(row.accessibleText());
        panel.setToolTipText(row.tooltip());
        for (int i = 0; i < labels.length; i++) {
            var label = labels[i];
            label.clear();
            label.setVisible(i < row.lines().size());
            if (!label.isVisible()) continue;
            label.setForeground(defaults.getForeground());
            label.setFont(list.getFont());
            for (Fragment fragment : row.lines().get(i)) {
                var style = fragment.style();
                if (selected && style == MATCH) style = SELECTED_MATCH;
                if (selected && style == GRAYED_SMALL_ATTRIBUTES) style = SMALL;
                label.append(fragment.text(), style);
            }
        }
        return panel;
    }

    static List<Row> rows(List<EntryData> entries, String executedQuery, ProgressIndicator indicator) {
        String query = normalize(executedQuery).strip();
        List<String> terms = query.isEmpty() ? List.of() : Arrays.stream(query.split("(?U)\\s+"))
                .filter(term -> !term.isEmpty()).distinct().toList();
        List<Row> rows = new ArrayList<>(entries.size());
        for (EntryData entry : entries) {
            indicator.checkCanceled();
            rows.add(prepare(entry, terms, indicator));
        }
        return rows;
    }

    private static Row prepare(EntryData entry, List<String> terms, ProgressIndicator indicator) {
        Set<String> missing = new LinkedHashSet<>(terms);
        List<List<Fragment>> lines = new ArrayList<>(4);
        List<Fragment> title = new ArrayList<>();
        field(title, entry.name(), REGULAR_BOLD_ATTRIBUTES, terms, missing);
        if (entry.httpMethod() != null) title.add(new Fragment("   " + entry.httpMethod(), REGULAR_ATTRIBUTES));
        if (entry.httpPath() != null) {
            title.add(new Fragment(" ", REGULAR_ATTRIBUTES));
            field(title, entry.httpPath(), REGULAR_ATTRIBUTES, terms, missing);
        }
        lines.add(title);

        var id = entry.identity();
        List<Fragment> identity = new ArrayList<>();
        field(identity, id.module(), GRAYED_SMALL_ATTRIBUTES, terms, missing);
        identity.add(new Fragment("  ·  ", GRAYED_SMALL_ATTRIBUTES));
        field(identity, id.qualifiedClass(), GRAYED_SMALL_ATTRIBUTES, terms, missing);
        identity.add(new Fragment("#", GRAYED_SMALL_ATTRIBUTES));
        field(identity, id.fullSignature(), GRAYED_SMALL_ATTRIBUTES, terms, missing);
        lines.add(identity);
        if (!entry.description().isBlank() && !entry.description().equals(entry.name())) {
            List<Fragment> description = new ArrayList<>();
            field(description, entry.description(), GRAYED_SMALL_ATTRIBUTES, terms, missing);
            lines.add(description);
        }

        List<Fragment> context = new ArrayList<>();
        if (!missing.isEmpty() && containsAny(entry.source().path(), missing)) {
            field(context, entry.source().path(), GRAYED_SMALL_ATTRIBUTES, terms, missing);
        }
        int shownTags = 0;
        for (String tag : entry.tags()) {
            if (missing.isEmpty() || shownTags == 3) break;
            indicator.checkCanceled();
            if (!containsAny(tag, missing)) continue;
            context.add(new Fragment(context.isEmpty() ? "#" : "  ·  #", GRAYED_SMALL_ATTRIBUTES));
            field(context, tag, GRAYED_SMALL_ATTRIBUTES, terms, missing);
            shownTags++;
        }
        if (!context.isEmpty()) lines.add(context);

        String text = String.join(" · ", lines.stream()
                .map(line -> String.join("", line.stream().map(Fragment::text).toList())).toList());
        String fullSignature = id.qualifiedClass() + "#" + id.fullSignature();
        String accessible = text.contains(fullSignature) ? text : text + " · " + fullSignature;
        return new Row(entry, lines, accessible, "<html>" + StringUtil.escapeXmlEntities(accessible) + "</html>");
    }

    private static boolean containsAny(String text, Set<String> terms) {
        String normalized = normalize(text);
        return terms.stream().anyMatch(normalized::contains);
    }

    private static void field(List<Fragment> line, String text, SimpleTextAttributes normal,
                              List<String> terms, Set<String> missing) {
        // Find context in the full field, but bound both the rendered text and its offset map.
        int first = -1;
        if (!terms.isEmpty()) {
            String normalized = normalize(text);
            for (String term : terms) {
                int at = normalized.indexOf(term);
                if (at >= 0) {
                    missing.remove(term);
                    if (first < 0 || at < first) first = at;
                }
            }
            // A normalized offset is not a source offset if an earlier character expanded or composed.
            if (first >= 0 && text.length() > SNIPPET && normalized.length() != text.length()) {
                int raw = firstLiteralOffset(text, terms);
                if (raw >= 0) first = raw;
            }
        }
        int start = text.length() > SNIPPET && first > 12 ? Math.min(text.length() - 1, first - 12) : 0;
        int end = Math.min(text.length(), start + SNIPPET);
        if (start > 0 && Character.isLowSurrogate(text.charAt(start))) start--;
        if (end < text.length() && Character.isLowSurrogate(text.charAt(end))) end++;
        String snippet = text.substring(start, end);
        if (start > 0) line.add(new Fragment("…", normal));
        BitSet hits = mark(snippet, terms);
        for (int i = 0; i < snippet.length();) {
            boolean hit = hits.get(i);
            int next = hit ? hits.nextClearBit(i) : hits.nextSetBit(i);
            if (next < 0) next = snippet.length();
            line.add(new Fragment(snippet.substring(i, next), hit ? MATCH : normal));
            i = next;
        }
        if (end < text.length()) line.add(new Fragment("…", normal));
    }

    private static int firstLiteralOffset(String text, List<String> terms) {
        int first = -1;
        for (String term : terms) {
            int at = StringUtil.indexOfIgnoreCase(text, term, 0);
            if (at >= 0 && (first < 0 || at < first)) first = at;
        }
        return first;
    }

    private static BitSet mark(String original, List<String> terms) {
        if (terms.isEmpty()) return new BitSet();
        String normalized = normalize(original);
        BitSet found = new BitSet(normalized.length());
        for (String term : terms) {
            for (int i = normalized.indexOf(term); i >= 0; i = normalized.indexOf(term, i + 1)) {
                found.set(i, i + term.length());
            }
        }
        if (found.isEmpty() || original.equalsIgnoreCase(normalized)) return found;
        // NFKC can compose several original characters or expand one ligature. Paint whole original
        // graphemes only when their normalized concatenation accounts for the offsets used by search.
        var characters = BreakIterator.getCharacterInstance(Locale.ROOT);
        characters.setText(original);
        BitSet mapped = new BitSet(original.length());
        int offset = 0;
        for (int from = characters.first(), to = characters.next(); to != BreakIterator.DONE;
             from = to, to = characters.next()) {
            String piece = normalize(original.substring(from, to));
            if (!normalized.startsWith(piece, offset)) return literalMarks(original, terms);
            int hit = found.nextSetBit(offset);
            if (hit >= 0 && hit < offset + piece.length()) mapped.set(from, to);
            offset += piece.length();
        }
        return offset == normalized.length() ? mapped : literalMarks(original, terms);
    }

    private static BitSet literalMarks(String original, List<String> terms) {
        BitSet marks = new BitSet(original.length());
        for (String term : terms) {
            for (int i = 0; i + term.length() <= original.length(); i++) {
                if (original.regionMatches(true, i, term, 0, term.length()) &&
                        normalize(original.substring(i, i + term.length())).equals(term)) marks.set(i, i + term.length());
            }
        }
        return marks;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
