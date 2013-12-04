package org.jboss.as.test.integration.logging.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;

/**
 * A simple abstract test class with helper methods.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractLoggingTest {

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "logging");

    static final String ENCODING = "utf-8";

    public static File prepareLogFile(final ManagementClient client,
                                      final String filename) throws IOException, MgmtOperationException {
        return getAbsoluteLogFilePath(client, filename);
    }

    public static File getAbsoluteLogFilePath(final ManagementClient client, final String filename) throws IOException, MgmtOperationException {
        final ModelNode address = PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.PATH, "jboss.server.log.dir")
                                                         ).toModelNode();
        final ModelNode op = Operations.createReadAttributeOperation(address, ModelDescriptionConstants.PATH);
        final ModelNode result = client.getControllerClient().execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return new File(Operations.readResult(result).asString(), filename);
        }
        throw new MgmtOperationException("Failed to read the path resource", op, result);
    }

    protected static String performCall(final String url) throws ExecutionException, IOException, TimeoutException {
        return HttpRequest.get(url, TimeoutUtil.adjust(10), TimeUnit.SECONDS);
    }

    protected static String performCall(final UrlBuilder builder) throws ExecutionException, IOException, TimeoutException {
        return performCall(builder.build());
    }

    public static class AddressBuilder {
        private PathAddress pathAddress;

        private AddressBuilder(final PathAddress pathAddress) {
            this.pathAddress = pathAddress;
        }

        public static AddressBuilder create() {
            return new AddressBuilder(PathAddress.pathAddress(SUBSYSTEM_PATH));
        }

        public static AddressBuilder create(final ModelNode base) {
            return new AddressBuilder(PathAddress.pathAddress(base));
        }

        public AddressBuilder add(final String key, final String name) {
            pathAddress = pathAddress.append(key, name);
            return this;
        }

        public ModelNode build() {
            return pathAddress.toModelNode();
        }
    }

    public static class UrlBuilder {
        private final URL url;
        private final String[] paths;
        private final Map<String, String> params;

        private UrlBuilder(final URL url, final String... paths) {
            this.url = url;
            this.paths = paths;
            params = new HashMap<String, String>();
        }

        public static UrlBuilder of(final URL url, final String... paths) {
            return new UrlBuilder(url, paths);
        }

        public UrlBuilder addParameter(final String key, final int value) {
            return addParameter(key, Integer.toString(value));
        }

        public UrlBuilder addParameter(final String key, final String value) {
            params.put(key, value);
            return this;
        }

        public String build() throws UnsupportedEncodingException {
            final StringBuilder result = new StringBuilder(url.toExternalForm());
            if (paths != null) {
                for (String path : paths) {
                    if (!path.startsWith("/")) {
                        result.append('/');
                    }
                    result.append(path);
                }
            }
            boolean isFirst = true;
            for (String key : params.keySet()) {
                if (isFirst) {
                    result.append('?');
                } else {
                    result.append('&');
                }
                final String value = params.get(key);
                result.append(URLEncoder.encode(key, ENCODING)).append('=').append(URLEncoder.encode(value, ENCODING));
                isFirst = false;
            }
            return result.toString();
        }
    }

}
