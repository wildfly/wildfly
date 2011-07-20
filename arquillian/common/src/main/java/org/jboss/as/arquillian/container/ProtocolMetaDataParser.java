package org.jboss.as.arquillian.container;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ProtocolMetaData} provider that uses the JBoss AS 7 admin API
 * {@link ModelControllerClient} to retrieve information about the deployments.
 * 
 * <p>In particular, this parser retrieves the context root of the web archives
 * and collects the registered servlets per each web context into the {@link HTTPContext}.</p>
 * 
 * <p>If the deployment metadata cannot be read, or for some reason an operation fails, a
 * {@link DeploymentException} will be thrown, indicating that the deployment failed with
 * an error.</p>
 *
 * @author <a href="http://community.jboss.org/people/aslak">Aslak Knutsen</a>
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
public class ProtocolMetaDataParser {

    private static final String SUBDEPLOYMENT = "subdeployment";
    private static final String WEB = "web";
    private static final String SERVLET = "servlet";

    private ModelControllerClient client;

    public ProtocolMetaDataParser(ModelControllerClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Client must be specified");
        }
        this.client = client;
    }

    public ProtocolMetaData parse(String deploymentName, HTTPContext context) throws DeploymentException {
        ProtocolMetaData protocol = new ProtocolMetaData();
        protocol.addContext(context);

        try {
            if (isWebArchive(deploymentName)) {
                extractWebArchiveContexts(context, deploymentName);
            } else if (isEnterpriseArchive(deploymentName)) {
                extractEnterpriseArchiveContexts(context, deploymentName);
            }
        } catch (Exception e) {
            throw new DeploymentException("Could not parse deployment metadata", e);
        }

        return protocol;
    }

    private void extractEnterpriseArchiveContexts(HTTPContext context,
            String deploymentName) throws Exception {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(CHILD_TYPE).set(SUBDEPLOYMENT);
        ModelNode result = executeForResult(operation);
        for (String subDeploymentName : result.keys()) {
            if (isWebArchive(subDeploymentName)) {
                extractEnterpriseWebArchiveContexts(context,
                        deploymentName, subDeploymentName);
            }
        }
    }

    private void extractWebArchiveContexts(HTTPContext context,
            String webDeploymentName) throws Exception {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, webDeploymentName);

        extractWebContext(context, webDeploymentName, address);
    }

    private void extractEnterpriseWebArchiveContexts(HTTPContext context,
            String enterpriseDeploymentName, String webDeploymentName) throws Exception {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, enterpriseDeploymentName);
        address.add(SUBDEPLOYMENT, webDeploymentName);

        extractWebContext(context, webDeploymentName, address);
    }

    private void extractWebContext(HTTPContext context, String deploymentName,
            ModelNode address) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set(SUBSYSTEM);
        operation.get(OP_ADDR).set(address);
        ModelNode result = executeForResult(operation);
        ModelNode webNode = result.get(WEB);
        if (webNode.isDefined()) {
            for (ModelNode servletNode : webNode.get(SERVLET).asList()) {
                for (String servletName : servletNode.keys()) {
                    context.add(new Servlet(servletName,
                            toContextName(deploymentName)));
                }
            }
        }

        context.add(new Servlet("default", toContextName(deploymentName)));
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
            throw new UnSuccessfulOperationException(result.get(FAILURE_DESCRIPTION) + " " + operation.toString());
        }
    }

    private static class UnSuccessfulOperationException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnSuccessfulOperationException(String message) {
            super(message);
        }
    }
}
