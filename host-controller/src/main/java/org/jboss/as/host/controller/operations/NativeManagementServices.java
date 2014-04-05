package org.jboss.as.host.controller.operations;

import java.util.List;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.host.controller.DomainModelControllerService;
import org.jboss.as.host.controller.HostControllerService;
import org.jboss.as.host.controller.jmx.RemotingConnectorService;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandlerFactoryService;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Utility class that installs remoting services needed by both the native and HTTP upgrade
 * based connector.
 *
 * @author Stuart Douglas
 */
public class NativeManagementServices {

    private static final int heartbeatInterval = 15000;
    private static final int WINDOW_SIZE = ProtocolChannelClient.Configuration.WINDOW_SIZE;

    private static final OptionMap SERVICE_OPTIONS = OptionMap.create(RemotingOptions.TRANSMIT_WINDOW_SIZE, WINDOW_SIZE,
                                                        RemotingOptions.RECEIVE_WINDOW_SIZE, WINDOW_SIZE);

    public static final OptionMap CONNECTION_OPTIONS = OptionMap.create(RemotingOptions.HEARTBEAT_INTERVAL, heartbeatInterval,
                                                        Options.READ_TIMEOUT, 45000);

    static synchronized void installRemotingServicesIfNotInstalled(final ServiceTarget serviceTarget,
                    final String hostName,
                    final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final ServiceRegistry serviceContainer, boolean onDemand) {

        if(serviceContainer.getService(ManagementRemotingServices.MANAGEMENT_ENDPOINT) == null) {

            ManagementChannelRegistryService.addService(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT);

            ManagementRemotingServices.installRemotingManagementEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                    hostName, EndpointService.EndpointType.MANAGEMENT, CONNECTION_OPTIONS, verificationHandler, newControllers);


            ManagementRemotingServices.installManagementChannelOpenListenerService(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                    ManagementRemotingServices.SERVER_CHANNEL,
                    ServerToHostOperationHandlerFactoryService.SERVICE_NAME, SERVICE_OPTIONS, verificationHandler, newControllers, onDemand);

            ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                    new ModelControllerClientOperationHandlerFactoryService(),
                    DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.MANAGEMENT_CHANNEL,
                    HostControllerService.HC_EXECUTOR_SERVICE_NAME, verificationHandler, newControllers);

            RemotingConnectorService.addService(serviceTarget, verificationHandler);

        }
    }
}
