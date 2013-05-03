package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostAdd extends AbstractAddStepHandler {

    static final HostAdd INSTANCE = new HostAdd();

    private HostAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        HostDefinition.ALIAS.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        final String name = address.getLastElement().getValue();
        List<String> aliases = HostDefinition.ALIAS.unwrap(context, model);
        final String serverName = parent.getLastElement().getValue();
        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, name);
        Host service = new Host(name, aliases == null ? new LinkedList<String>() : aliases);
        final ServiceBuilder<Host> builder = context.getServiceTarget().addService(virtualHostServiceName, service)
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServer())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowService())
                .addAliases(WebHost.SERVICE_NAME.append(name));

        if (aliases != null) {
            for (String alias : aliases) {
                builder.addAliases(WebHost.SERVICE_NAME.append(alias));
            }
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<Host> serviceController = builder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}
