package com.example.study_buddy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Utility helper to extract text content from files or notebook pages
 * to serve as source material for AI quiz generation.
 */
public class QuizSourceHelper {

    /**
     * Reads text content from a chosen file (supports .pdf, .txt, .md, .json, etc.).
     */
    public static String readFileContent(File file) throws IOException {
        if (file == null || !file.exists()) {
            return "";
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(file)) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                return stripper.getText(document);
            }
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Extracts readable text from a Study Buddy Page's blocks.
     */
    public static String extractPageContent(Page page) {
        if (page == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Page Title: ").append(page.getTitle()).append("\n\n");

        List<PageBlock> blocks = PageBlock.deserializeList(page.getContentJson());
        for (PageBlock b : blocks) {
            if (PageBlock.TYPE_TEXT.equalsIgnoreCase(b.getType())) {
                if (b.getContent() != null && !b.getContent().trim().isEmpty()) {
                    sb.append(b.getContent().trim()).append("\n\n");
                }
            } else if (PageBlock.TYPE_CODE.equalsIgnoreCase(b.getType())) {
                if (b.getContent() != null && !b.getContent().trim().isEmpty()) {
                    sb.append("Code (").append(b.getExtra()).append("):\n")
                      .append(b.getContent().trim()).append("\n\n");
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Generates a compact summary string of source metadata.
     */
    public static String formatSourceSummary(String sourceName, String text) {
        if (text == null || text.trim().isEmpty()) {
            return sourceName + " (empty)";
        }
        int charCount = text.length();
        int wordCount = text.trim().split("\\s+").length;
        return String.format("%s (%d chars, ~%d words)", sourceName, charCount, wordCount);
    }
}
