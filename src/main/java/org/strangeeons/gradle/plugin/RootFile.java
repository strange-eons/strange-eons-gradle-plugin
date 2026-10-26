package org.strangeeons.gradle.plugin;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text operations on an {@code eons-plugin} root file. */
final class RootFile {

    /** Same shape as {@code CatalogID.ID_PAT}; group 1 is everything up to the date. */
    private static final Pattern CATALOG_ID = Pattern.compile(
            "(CATALOGU?E?ID\\{[-\\da-fA-F]+:)\\d+-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+(\\})",
            Pattern.CASE_INSENSITIVE);

    private RootFile() {
    }

    /**
     * Formats a time the way {@code CatalogID.toDateString()} does: UTC,
     * 0-based month, no zero padding.
     */
    static String dateString(Instant time) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        c.setTimeInMillis(time.toEpochMilli());
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-"
                + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND) + "-"
                + c.get(Calendar.MILLISECOND);
    }

    /** Replaces the date of every catalog ID in {@code text}, keeping its UUID. */
    static String stamp(String text, Instant time) {
        return CATALOG_ID.matcher(text).replaceAll("$1" + Matcher.quoteReplacement(dateString(time)) + "$2");
    }

    /**
     * Guesses the bundle type the way SE's {@code MakeBundle} does for
     * class-based plug-ins: no plug-in line means a library, a class naming
     * a theme is a theme, anything else is an extension.
     */
    static String guessType(List<String> lines) {
        String first = firstPluginIdentifier(lines);
        if (first == null) {
            return "selibrary";
        }
        return first.toLowerCase(Locale.ROOT).contains("theme") ? "setheme" : "seext";
    }

    /**
     * Returns the first plug-in line: a non-blank, non-comment line that is
     * not a {@code key = value} pair or its continuation.
     */
    static String firstPluginIdentifier(List<String> lines) {
        boolean continuation = false;
        for (String raw : lines) {
            String line = raw.strip();
            boolean wasContinuation = continuation;
            continuation = line.endsWith("\\");
            if (wasContinuation || line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            if (line.matches("[^:=\\s]+\\s*[=:].*") && !line.startsWith("res:") && !line.startsWith("script:")
                    && !line.startsWith("class:")) {
                continue;
            }
            return line;
        }
        return null;
    }
}
