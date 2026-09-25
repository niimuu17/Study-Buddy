package com.example.study_buddy;

/**
 * Represents an attached document/file associated with a Topic (e.g. PDF, PPTX, DOCX).
 */
public class TopicFile {
    private int id;
    private int topicId;
    private String originalName;
    private String storedFilePath;
    private String fileExtension;
    private long fileSizeBytes;
    private String createdAt;

    public TopicFile(int id, int topicId, String originalName, String storedFilePath,
                     String fileExtension, long fileSizeBytes, String createdAt) {
        this.id = id;
        this.topicId = topicId;
        this.originalName = originalName;
        this.storedFilePath = storedFilePath;
        this.fileExtension = fileExtension != null ? fileExtension.toLowerCase() : "";
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = createdAt;
    }

    public TopicFile(int id, int topicId, String originalName, String storedFilePath,
                     String fileExtension, long fileSizeBytes) {
        this(id, topicId, originalName, storedFilePath, fileExtension, fileSizeBytes, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredFilePath() {
        return storedFilePath;
    }

    public void setStoredFilePath(String storedFilePath) {
        this.storedFilePath = storedFilePath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns a human-readable file size string (e.g. 512 KB, 3.4 MB).
     */
    public String getFormattedSize() {
        if (fileSizeBytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(fileSizeBytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.1f %s", fileSizeBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Returns an emoji or icon string matching the file format.
     */
    public String getFileIcon() {
        String ext = fileExtension.replace(".", "").toLowerCase();
        return switch (ext) {
            case "pdf" -> "📕";
            case "ppt", "pptx" -> "📊";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx", "csv" -> "📗";
            case "png", "jpg", "jpeg", "gif", "webp" -> "🖼";
            case "zip", "rar", "7z", "tar", "gz" -> "📦";
            case "txt", "md" -> "📝";
            case "java", "py", "c", "cpp", "js", "html", "css" -> "💻";
            default -> "📎";
        };
    }
}
