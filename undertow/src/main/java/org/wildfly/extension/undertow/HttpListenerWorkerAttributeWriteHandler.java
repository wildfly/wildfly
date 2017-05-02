package org.wildfly.extension.undertow;

import java.util.logging.Level;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.logging.UndertowLogger;

public class HttpListenerWorkerAttributeWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public HttpListenerWorkerAttributeWriteHandler(AttributeDefinition... definitions) {
        super(definitions);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
        try {
            final ModelNode remotingWorker = getRemotingWorker(context);
            if (isHttpUpgradeEnabled(context) && !isNewWorkerEqualToRemoting(remotingWorker, newValue)) {
                context.addResponseWarning(Level.WARNING, UndertowLogger.ROOT_LOGGER
                        .workerValueInHTTPListenerMustMatchRemoting(asString(newValue), asString(remotingWorker)));
                context.setRollbackOnly();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String asString(final ModelNode value) {
        if (value.isDefined()) {
            return value.asString();
        } else {
            return "undefined";
        }
    }

    private boolean isHttpUpgradeEnabled(final OperationContext context) {
        // there are two params it seems:
        // "http-upgrade" => {"enabled" => true},
        // "http-upgrade-enabled" => true,
        final ModelNode managementInterface = context
                .readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement("core-service", "management"),
                        PathElement.pathElement("management-interface", "http-interface")))
                .getModel();
        final ModelNode httpUpgradeEnabled = managementInterface.get("http-upgrade-enabled");
        return !httpUpgradeEnabled.isDefined() || httpUpgradeEnabled.asBoolean(); // undefined == true
    }

    private boolean isNewWorkerEqualToRemoting(final ModelNode remotingWorker, final ModelNode newValue) {
        if ((!newValue.isDefined() && !remotingWorker.isDefined())
                || (newValue.isDefined() && remotingWorker.isDefined() && newValue.asString().equals(remotingWorker.asString()))) {
            return true;
        } else {
            return false;
        }
    }

    private ModelNode getRemotingWorker(final OperationContext context) {
        final ModelNode remotingConfiguration = context
                .readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement("subsystem", "remoting"),
                        PathElement.pathElement("configuration", "endpoint")))
                .getModel();
        return remotingConfiguration.get("worker");

    }
}
