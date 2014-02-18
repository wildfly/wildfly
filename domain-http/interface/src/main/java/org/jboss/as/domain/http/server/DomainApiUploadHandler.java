package org.jboss.as.domain.http.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_MECHANISM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_DMR_ENCODED;
import static org.jboss.as.domain.http.server.Constants.CONTENT_DISPOSITION;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.domain.http.server.multipart.BoundaryDelimitedInputStream;
import org.jboss.as.domain.http.server.multipart.MimeHeaderParser;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;
import org.jboss.dmr.ModelNode;

/**
 * Generic http POST handler accepting a single operation and multiple input streams passed as part of
 * a {@code multipart/form-data} message.
 *
 * The operation is required, the attachment streams are optional.
 *
 * Content-Disposition: form-data; name="operation"
 * (optional) Content-Type: application/dmr-encoded
 *
 * Content-Disposition: form-data; name="..."; filename="..."
 *
 * @author Emanuel Muckenhuber
 */
class DomainApiUploadHandler implements HttpHandler {

    private static final String OPERATION = "operation";

    private static final int BUFFER_SIZE = 8192;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final Pattern MULTIPART_FD_BOUNDARY =  Pattern.compile("^multipart/form-data.*;\\s*boundary=(.*)$");

    private final File tempFileLocation;
    private final ModelControllerClient modelController;

    public DomainApiUploadHandler(final ModelControllerClient modelController) {
        this.tempFileLocation = new File(SecurityActions.getProperty("java.io.tmpdir"));
        this.modelController = modelController;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        final List<File> tempFiles = new ArrayList<File>();
        final ModelNode operation = new ModelNode();
        final OperationBuilder builder = new OperationBuilder(operation);

        try {
            // Process the request
            boolean encode = processRequest(exchange, operation, builder, tempFiles);
            // Execute the operation
            operation.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.HTTP.toString());
            final ModelNode response = modelController.execute(builder.build());
            // write the response
            writeResponse(exchange, false, false, response, OK, encode);

        } catch (Throwable t) {
            sendError(exchange, t);
        }  finally {
            // close the input stream
            safeClose(exchange.getRequestBody());
            for (final File file : tempFiles) {
                file.delete();
            }
        }
    }

    /**
     * Process the multipart/form-data request body.
     *
     * @param exchange the http exchange
     * @param operation the model operation
     * @param builder   the operation builder
     * @param tempFiles list of temp files
     * @return whether the response should be dmr encoded or not
     * @throws IOException
     */
    private boolean processRequest(final HttpExchange exchange, final ModelNode operation, final OperationBuilder builder, final List<File> tempFiles) throws IOException {
        final String contentType = exchange.getRequestHeaders().getFirst(CONTENT_TYPE);
        final byte[] buffer = new byte[BUFFER_SIZE];
        // Check and resolve boundary
        final String boundary = resolveBoundary(contentType);
        final BoundaryDelimitedInputStream stream = new BoundaryDelimitedInputStream(exchange.getRequestBody(), boundary.getBytes(US_ASCII));

        // Eat preamble
        drain(stream, buffer);

        // From here on out a boundary is prefixed with a CRLF that should be skipped
        stream.setBoundary(("\r\n" + boundary).getBytes(US_ASCII));

        boolean encode = false;
        while (!stream.isOuterStreamClosed()) {

            final MimeHeaderParser.ParseResult result = MimeHeaderParser.parseHeaders(stream);
            if (result.eof()) continue; // Skip content-less part

            final Headers partHeaders = result.headers();
            // Content-Disposition: form-data
            final String disposition = partHeaders.getFirst(CONTENT_DISPOSITION);
            if (disposition != null && disposition.startsWith("form-data")) {
                final String name = getValueFromHeader(disposition, "name");
                final String fileName = getValueFromHeader(disposition, "filename");

                if (OPERATION.equals(name)) {
                    // Process the operation
                    encode = processOperation(operation, partHeaders, stream, buffer);
                } else if (fileName != null) {
                    // Process files
                    final File file = File.createTempFile("http", "upload", tempFileLocation);
                    tempFiles.add(file);
                    final OutputStream os = new FileOutputStream(file);
                    try {
                        copyStream(stream, os, buffer);
                        os.close();
                        builder.addFileAsAttachment(file);
                    } finally {
                        safeClose(os);
                    }
                } else {
                    drain(stream, buffer);
                }
            } else {
                throw HttpServerMessages.MESSAGES.unsupportedContentDisposition(disposition);
            }
        }
        return encode;
    }

    private static boolean processOperation(final ModelNode operation, final Headers partHeaders, final InputStream stream, final byte[] buffer) throws IOException {
        // Process the content type for dmr encoded application
        final String contentType = partHeaders.getFirst(CONTENT_TYPE);
        if (APPLICATION_DMR_ENCODED.equals(contentType)) {
            // We are not allowed to close the BoundaryDelimitedInputStream so wrap it
            operation.set(ModelNode.fromBase64(new Base64InputStreamWrapper(stream)));
            return true;
        } else {
            // Copy the model into memory
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            copyStream(stream, os, buffer);
            // Create the operation
            final Charset charset = getCharset(contentType);
            operation.set(ModelNode.fromJSONString(new String(os.toByteArray(), charset)));
            return false;
        }
    }

    private static void copyStream(final InputStream is, final OutputStream os, final byte[] buffer) throws IOException {
        int cnt;
        while ((cnt = is.read(buffer)) != -1) {
            os.write(buffer, 0, cnt);
        }
    }

    private static Charset getCharset(final String contentType) {
        Charset charset = US_ASCII;
        if (contentType != null) {
            String cs = getValueFromHeader(contentType, "charset");
            if (cs != null) {
                charset = Charset.forName(cs);
            }
        }
        return charset;
    }

    private void sendError(final HttpExchange http, Throwable t) throws IOException {
        ModelNode response = new ModelNode();
        response.set(t.getMessage());
        writeResponse(http, false, true, response, INTERNAL_SERVER_ERROR, false);
    }

    public static String getValueFromHeader(final String header, final String key) {
        int pos = header.indexOf(key + '=');
        if (pos == -1) {
            return null;
        }

        int end;
        int start = pos + key.length() + 1;
        if (header.charAt(start) == '"') {
            start++;
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == '"') {
                    break;
                }
            }
            return header.substring(start, end);
        } else {
            //no quotes
            for (end = start; end < header.length(); ++end) {
                char c = header.charAt(end);
                if (c == ' ' || c == '\t') {
                    break;
                }
            }
            return header.substring(start, end);
        }
    }

    static String resolveBoundary(final String contentType) {
        if (contentType == null) {
            throw MESSAGES.invalidContentType();
        }
        final Matcher matcher = MULTIPART_FD_BOUNDARY.matcher(contentType);
        if (!matcher.matches()) {
            throw MESSAGES.invalidContentType(contentType);
        }
        return "--" + matcher.group(1);
    }

    static void drain(final InputStream is, final byte[] buffer) throws IOException {
        while (is.read(buffer) != -1) {
            //
        }
    }

    static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            HttpServerLogger.ROOT_LOGGER.debugf(e, "failed to close stream");
        }
    }

    static class Base64InputStreamWrapper extends FilterInputStream {
        Base64InputStreamWrapper(InputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            // don't close the stream
        }
    }

}
