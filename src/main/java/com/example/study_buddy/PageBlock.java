package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a modular content block inside a Page (Note, Code snippet, or Screenshot/Image).
 */
public class PageBlock {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_CODE = "CODE";
    public static final String TYPE_IMAGE = "IMAGE";

    private String id;
    private String type;
    private String content;
    private String extra; // Language for CODE, Caption for IMAGE

    public PageBlock(String id, String type, String content, String extra) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        this.type = (type != null) ? type : TYPE_TEXT;
        this.content = (content != null) ? content : "";
        this.extra = (extra != null) ? extra : "";
    }

    public PageBlock(String type, String content, String extra) {
        this(UUID.randomUUID().toString(), type, content, extra);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    /**
     * Serializes a list of PageBlocks into a JSON array string.
     */
    public static String serializeList(List<PageBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < blocks.size(); i++) {
            PageBlock b = blocks.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escapeJson(b.id)).append("\",\n");
            sb.append("    \"type\": \"").append(escapeJson(b.type)).append("\",\n");
            sb.append("    \"content\": \"").append(escapeJson(b.content)).append("\",\n");
            sb.append("    \"extra\": \"").append(escapeJson(b.extra)).append("\"\n");
            sb.append("  }");
            if (i < blocks.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Deserializes a JSON array string into a list of PageBlocks.
     * Gracefully handles legacy plain text by wrapping it in a single TEXT block.
     */
    public static List<PageBlock> deserializeList(String json) {
        List<PageBlock> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }

        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            // Legacy plain text content -> convert to single text block
            list.add(new PageBlock(TYPE_TEXT, trimmed, ""));
            return list;
        }

        try {
            // Parse individual JSON block objects
            int startIdx = 0;
            while ((startIdx = trimmed.indexOf('{', startIdx)) != -1) {
                int endIdx = findMatchingBrace(trimmed, startIdx);
                if (endIdx == -1) break;

                String objStr = trimmed.substring(startIdx + 1, endIdx);
                String id = extractField(objStr, "id");
                String type = extractField(objStr, "type");
                String content = extractField(objStr, "content");
                String extra = extractField(objStr, "extra");

                if (type == null || type.isEmpty()) type = TYPE_TEXT;
                list.add(new PageBlock(id, type, content, extra));
                startIdx = endIdx + 1;
            }
        } catch (Exception e) {
            // Fallback to text block on parsing error
            list.clear();
            list.add(new PageBlock(TYPE_TEXT, trimmed, ""));
        }

        return list;
    }

    private static int findMatchingBrace(String s, int openPos) {
        int depth = 0;
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static String extractField(String objStr, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyPos = objStr.indexOf(key);
        if (keyPos == -1) return "";

        int colonPos = objStr.indexOf(':', keyPos + key.length());
        if (colonPos == -1) return "";

        int quoteStart = objStr.indexOf('"', colonPos + 1);
        if (quoteStart == -1) return "";

        StringBuilder val = new StringBuilder();
        boolean escape = false;
        for (int i = quoteStart + 1; i < objStr.length(); i++) {
            char c = objStr.charAt(i);
            if (escape) {
                switch (c) {
                    case '"' -> val.append('"');
                    case '\\' -> val.append('\\');
                    case 'n' -> val.append('\n');
                    case 'r' -> val.append('\r');
                    case 't' -> val.append('\t');
                    default -> val.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                val.append(c);
            }
        }
        return val.toString();
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
