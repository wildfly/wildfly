/**
 *
 */
package org.jboss.as.domain.controller.operations.deployment;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

/**
 * Utility method for storing deployment content.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadUtil {

    private DeploymentUploadUtil() {
    }

    public static byte[] storeDeploymentContent(OperationAttachments context, ModelNode operation, ContentRepository contentRepository) throws IOException, OperationFailedException {
        InputStream in = getContents(context, operation);
        return contentRepository.addContent(in);
    }

    private static InputStream getContents(OperationAttachments context, ModelNode operation) throws OperationFailedException {
        InputStream in = null;
        String message = "";
        if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
            if (streamIndex > context.getInputStreams().size() - 1) {
                IllegalArgumentException e = new IllegalArgumentException("Invalid " + INPUT_STREAM_INDEX + "=" + streamIndex + ", the maximum index is " + (context.getInputStreams().size() - 1));
                throw createFailureException(e, message);
            }
            message = "Null stream at index " + streamIndex;
            in = context.getInputStreams().get(streamIndex);
        } else if (operation.hasDefined(BYTES)) {
            in = new ByteArrayInputStream(operation.get(BYTES).asBytes());
            message = "Invalid byte stream.";
        } else if (operation.hasDefined(URL)) {
            final String urlSpec = operation.get(URL).asString();
            try {
                message = "Invalid url stream.";
                in = new URL(urlSpec).openStream();
            } catch (MalformedURLException e) {
                throw createFailureException(message);
            } catch (IOException e) {
                throw createFailureException(message);
            }
        }
        if (in == null) {
            throw createFailureException(message);
        }
        return in;
    }

    private static OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }

    private static OperationFailedException createFailureException(Throwable cause, String msg) {
        return new OperationFailedException(cause, new ModelNode().set(msg));
    }
}
