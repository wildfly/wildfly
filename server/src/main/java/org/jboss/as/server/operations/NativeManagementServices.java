package org.jboss.as.server.operations;

import java.util.List;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.Services;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;

/**
 * Utility class that installs remoting services needed by both the native and HTTP upgrade
 * based connector.
 *
 * @author Stuart Douglas
 */
class NativeManagementServices {

    private static final int WINDOW_SIZE = ProtocolChannelClient.Configuration.WINDOW_SIZE;
    private static final OptionMap OPTIONS = OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, WINDOW_SIZE);

    static synchronized void installRemotingServicesIfNotInstalled(final ServiceTarget serviceTarget,
                                                                   final String hostName,
                                                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final ServiceRegistry serviceContainer) {

        if (serviceContainer.getService(ManagementRemotingServices.MANAGEMENT_ENDPOINT) == null) {

            ManagementChannelRegistryService.addService(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT);

            ManagementRemotingServices.installRemotingManagementEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT, hostName, EndpointService.EndpointType.MANAGEMENT, OPTIONS, verificationHandler, newControllers);


            ManagementRemotingServices.installManagementChannelServices(serviceTarget,
                    ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                    new ModelControllerClientOperationHandlerFactoryService(),
                    Services.JBOSS_SERVER_CONTROLLER,
                    ManagementRemotingServices.MANAGEMENT_CHANNEL,
                    Services.JBOSS_SERVER_EXECUTOR,
                    verificationHandler,
                    newControllers);

        }
    }
}
