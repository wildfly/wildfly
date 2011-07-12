package org.jboss.as.arquillian.container;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class ProtocolMetaDataParser {

    private ModelControllerClient client;

    public ProtocolMetaDataParser(ModelControllerClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Client must be specified");
        }
        this.client = client;
    }

    public ProtocolMetaData parse(String deploymentName) {
        ProtocolMetaData protocol = new ProtocolMetaData();
        HTTPContext context = new HTTPContext("localhost", 8080);
        protocol.addContext(context);

        if (isWebArchive(deploymentName)) {
            extractWebArchiveContexts(context, deploymentName);
        } else if (isEnterpriseArchive(deploymentName)) {
            extractEnterpriseArchiveContexts(context, deploymentName);
        }

        return protocol;
    }

    private void extractEnterpriseArchiveContexts(HTTPContext context,
            String deploymentName) {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(CHILD_TYPE).set("subdeployment");
        try {
            ModelNode result = executeForResult(operation);
            for (String subDeploymentName : result.keys()) {
                if (isWebArchive(subDeploymentName)) {
                    extractEnterpriseWebArchiveContexts(context,
                            deploymentName, subDeploymentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractWebArchiveContexts(HTTPContext context,
            String webDeploymentName) {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, webDeploymentName);
        address.add(SUBSYSTEM, "web");

        extractWebContext(context, webDeploymentName, address);
    }

    private void extractEnterpriseWebArchiveContexts(HTTPContext context,
            String enterpriseDeploymentName, String webDeploymentName) {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, enterpriseDeploymentName);
        address.add("subdeployment", webDeploymentName);
        address.add(SUBSYSTEM, "web");

        extractWebContext(context, webDeploymentName, address);
    }

    private void extractWebContext(HTTPContext context, String deploymentName,
            ModelNode address) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);

        try {
            ModelNode result = executeForResult(operation);
            for (ModelNode servletNode : result.get("servlet").asList()) {
                for (String servletName : servletNode.keys()) {
                    context.add(new Servlet(servletName,
                            toContextName(deploymentName)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isEnterpriseArchive(String deploymentName) {
        return deploymentName.endsWith(".ear");
    }

    private boolean isWebArchive(String deploymentName) {
        return deploymentName.endsWith(".war");
    }

    private String toContextName(String deploymentName) {
        String correctedName = deploymentName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }
        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0,
                    correctedName.lastIndexOf("."));
        }
        return correctedName;
    }

    private ModelNode executeForResult(final ModelNode operation)
            throws Exception {
        final ModelNode result = client.execute(operation);
        checkSuccessful(result, operation);
        return result.get(RESULT);
    }

    static void checkSuccessful(final ModelNode result,
            final ModelNode operation) throws UnSuccessfulOperationException {
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new UnSuccessfulOperationException();
        }
    }

    private static class UnSuccessfulOperationException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
