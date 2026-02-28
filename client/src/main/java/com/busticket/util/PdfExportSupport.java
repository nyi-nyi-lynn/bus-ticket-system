package com.busticket.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class PdfExportSupport {
    private PdfExportSupport() {
    }

    public static File chooseExportTarget(Window owner, String title, String extension, String initialFileName) {
        String normalizedExtension = normalizeExtension(extension);

        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                normalizedExtension.toUpperCase(Locale.ROOT) + " Files",
                "*." + normalizedExtension
        ));
        chooser.setInitialFileName(normalizeInitialFileName(initialFileName, normalizedExtension));

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }

        String suffix = "." + normalizedExtension;
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(suffix)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + suffix);
    }

    public static void writeText(PDPageContentStream content, float x, float y, String text) throws IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(sanitizePdfText(text));
        content.endText();
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "pdf";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private static String normalizeInitialFileName(String initialFileName, String extension) {
        if (initialFileName == null || initialFileName.isBlank()) {
            return "export." + extension;
        }
        String trimmed = initialFileName.trim();
        String suffix = "." + extension;
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            return trimmed;
        }
        return trimmed + suffix;
    }

    private static String sanitizePdfText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        String normalized = value
                .replace("\u2192", "->")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');

        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= 32 && ch <= 126) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }

        String sanitized = builder.toString().trim();
        return sanitized.isEmpty() ? "-" : sanitized;
    }
}
