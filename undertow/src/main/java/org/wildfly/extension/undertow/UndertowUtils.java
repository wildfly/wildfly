package org.wildfly.extension.undertow;

public final class UndertowUtils {

    private UndertowUtils() {
    }

    /**
     * Normalizes the path to ensure it starts with a leading slash.
     *
     * @param path the path to normalize
     * @return the normalized path with a leading slash, or {@code null} if the input path was null
     */
    public static String normalizePath(String path) {
        if (path == null) return null;
        return path.startsWith("/") ? path : "/" + path;
    }

}
