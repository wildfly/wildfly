package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.DefaultAddHandler;
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
class LocationAdd extends DefaultAddHandler {
    static LocationAdd INSTANCE = new LocationAdd();

    private LocationAdd() {
        super(LocationDefinition.HANDLER);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress hostAddress = address.subAddress(0, address.size() - 1);
        final PathAddress serverAddress = hostAddress.subAddress(0, hostAddress.size() - 1);
        final String name = address.getLastElement().getValue();
        final String handler = LocationDefinition.HANDLER.resolveModelAttribute(context, model).asString();


        final LocationService service = new LocationService(name);
        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();
        final ServiceName hostServiceName = UndertowService.virtualHostName(serverName, hostName);
        final ServiceName serviceName = UndertowService.locationServiceName(serverName, hostName, name);
        final ServiceBuilder<LocationService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(hostServiceName, Host.class, service.getHost())
                .addDependency(UndertowService.HANDLER.append(handler), HttpHandler.class, service.getHttpHandler())
                .addAliases(WebHost.SERVICE_NAME.append(name));

        builder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<LocationService> serviceController = builder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }

    }
}
