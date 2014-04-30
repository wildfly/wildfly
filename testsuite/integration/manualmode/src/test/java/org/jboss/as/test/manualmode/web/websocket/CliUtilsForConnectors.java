package org.jboss.as.test.manualmode.web.websocket;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
public class CliUtilsForConnectors {
    private static Logger log = Logger.getLogger(CliUtilsForConnectors.class);

    public static String getConnectorProtocol(ModelControllerClient client, String connectorName) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(getConnectorAddress(connectorName));
        op.get(NAME).set("protocol");
        ModelNode result =  client.execute(new OperationBuilder(op).build());
        checkOpResult(result);
        return result.get("result").asString();
    }

    public static void defineConnectorProtocol(ModelControllerClient client, String connectorName, String protocol) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(getConnectorAddress(connectorName));
        op.get(NAME).set("protocol");
        op.get(VALUE).set(protocol);
        ModelNode result =  client.execute(new OperationBuilder(op).build());
        checkOpResult(result);
    }

    public static void reload(ModelControllerClient client) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set("reload");
        client.execute(new OperationBuilder(op).build());
        CustomCLIExecutor.waitForServerToReload(30000, null);
    }

    private static ModelNode getConnectorAddress(String connectorName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "web");
        address.add("connector", connectorName);
        address.protect();
        return address;
    }


    public static void checkOpResult(final ModelNode result) {
        log.info("Whole result: " + result);
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                log.info(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }
}
