package org.jboss.as.test.integration.management;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Shared class that provides utility methods for executing management operations
 *
 * @author Stuart Douglas
 */
public class ManagementOperations {

    private ManagementOperations() {

    }

    public static void executeOperations(final ModelControllerClient client, final List<ModelNode> operations) throws IOException, MgmtOperationException {
        for(ModelNode op : operations) {
            executeOperation(client, op);
        }
    }

    /**
     * Executes a management operation and returns the 'result' ModelNode of the server output. If the operation fails an exception will be thrown
     */
    public static ModelNode executeOperation(final ModelControllerClient client, final ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(client, op, true);
    }

    /**
     * Executes a management operation and returns the 'result' ModelNode of the server output. If the operation fails an exception will be thrown
     */
    public static ModelNode executeOperation(final ModelControllerClient client, final Operation op) throws IOException, MgmtOperationException {
        return executeOperation(client, op, true);
    }


    /**
     * Executes a management operation and returns the raw ModelNode that is returned from the server.
     *
     * It is up to the client to check it the result is a success or not
     *
     */
    public static ModelNode executeOperationRaw(final ModelControllerClient client, final ModelNode op) throws IOException {
        try {
            return executeOperation(client, op, false);
        } catch (MgmtOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static ModelNode executeOperation(final ModelControllerClient client, final ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        ModelNode ret = client.execute(op);
        if (!unwrapResult) return ret;

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), op, ret);
        }
        return ret.get(RESULT);
    }


    private static ModelNode executeOperation(final ModelControllerClient client, final Operation op, boolean unwrapResult) throws IOException, MgmtOperationException {
        ModelNode ret = client.execute(op);
        if (!unwrapResult) return ret;

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), op.getOperation(), ret);
        }
        return ret.get(RESULT);
    }
}
