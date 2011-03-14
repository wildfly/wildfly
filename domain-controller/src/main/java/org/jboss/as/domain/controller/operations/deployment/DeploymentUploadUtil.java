/**
 *
 */
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Utility method for storing deployment content.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadUtil {

    private DeploymentUploadUtil() {
    }

    public static byte[] storeDeploymentContent(OperationAttachments context, ModelNode operation, DeploymentRepository deploymentRepository) throws IOException {
        InputStream in = getContents(context, operation);
        return deploymentRepository.addDeploymentContent(in);
    }

    private static InputStream getContents(OperationAttachments context, ModelNode operation) {
        int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
        if (streamIndex > context.getInputStreams().size() - 1) {
            throw new IllegalArgumentException("Invalid " + INPUT_STREAM_INDEX + "=" + streamIndex + ", the maximum index is " + (context.getInputStreams().size() - 1));
        }

        InputStream in = context.getInputStreams().get(streamIndex);
        if (in == null) {
            throw new IllegalStateException("Null stream at index " + streamIndex);
        }
        return in;
    }
}
