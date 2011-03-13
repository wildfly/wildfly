package org.jboss.as.domain.http.server.multipart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class for extracted multipart header information from a multipart/form-data request.
 *
 * @author Jonathan Pearlin
 */
public class MultipartHeader {

    /**
     * Separator token used to split the header into lines.
     */
    private static final String HEADER_SPLIT_TOKEN = "\\r\\n";

    /**
     * Content-Disposition Regular Expression Pattern.
     */
    private static final Pattern CONTENT_DISPOSTION = Pattern.compile("Content-Disposition:\\s(.*)");

    /**
     * Content-Type Regular Expression Pattern.
     */
    private static final Pattern CONTENT_TYPE = Pattern.compile("Content-Type:\\s(.*)");

    /**
     * The extracted value from the "Content-Type" field in the POST header.
     */
    private final String contentType;

    /**
     * The extracted value from the "Content-Disposition" field in the POST header.
     */
    private final String contentDisposition;

    /**
     * Constructs a new <code>MultipartHeaders</code> object from the extracted header bytes.
     *
     * @param header
     */
    public MultipartHeader(final byte[] header) {
        final String[] headerLines = new String(header).split(HEADER_SPLIT_TOKEN);
        contentDisposition = find(headerLines, CONTENT_DISPOSTION, 1);
        contentType = find(headerLines, CONTENT_TYPE, 1);
    }

    /**
     * Finds the match to the supplied pattern in the string array, if one exists.
     *
     * @param headerLines The list of input to be matched against.
     * @param pattern The pattern used to perform the match.
     * @param groupId The group to select from the matched pattern, if present.
     * @return The matched group, or <code>null</code> if no such match exists.
     */
    private String find(final String[] headerLines, final Pattern pattern, final int groupId) {
        String value = null;
        for (final String line : headerLines) {
            final Matcher m = pattern.matcher(line);
            if (m.matches()) {
                value = m.group(groupId);
            }
        }
        return value;
    }

    /**
     * Returns the content type value from the header.
     *
     * @return The content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the content disposition string value from the header.
     *
     * @return The content disposition.
     */
    public String getContentDisposition() {
        return contentDisposition;
    }
}