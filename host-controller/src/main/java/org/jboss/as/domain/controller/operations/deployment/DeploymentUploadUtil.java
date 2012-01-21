/**
 *
 */
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Utility method for storing deployment content.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadUtil {

    private DeploymentUploadUtil() {
    }

    public static byte[] storeDeploymentContent(OperationContext context, ModelNode operation, ContentRepository contentRepository) throws IOException, OperationFailedException {
        InputStream in = getContents(context, operation);
        return contentRepository.addContent(in);
    }

    private static InputStream getContents(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(! operation.hasDefined(CONTENT)) {
            createFailureException(MESSAGES.invalidContentDeclaration());
        }
        return getInputStream(context, operation.require(CONTENT).get(0));
    }

    private static InputStream getInputStream(OperationContext context, ModelNode content) throws OperationFailedException {
        InputStream in = null;
        String message = "";
        if (content.hasDefined(INPUT_STREAM_INDEX)) {
            int streamIndex = content.get(INPUT_STREAM_INDEX).asInt();
            if (streamIndex > context.getAttachmentStreamCount() - 1) {
                message = MESSAGES.invalidValue(INPUT_STREAM_INDEX, streamIndex, (context.getAttachmentStreamCount() - 1));
                throw createFailureException(message);
            }
            message = MESSAGES.nullStream(streamIndex);
            in = context.getAttachmentStream(streamIndex);
        } else if (content.hasDefined(BYTES)) {
            in = new ByteArrayInputStream(content.get(BYTES).asBytes());
            message = MESSAGES.invalidByteStream();
        } else if (content.hasDefined(URL)) {
            final String urlSpec = content.get(URL).asString();
            try {
                message = MESSAGES.invalidUrlStream();
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
}
