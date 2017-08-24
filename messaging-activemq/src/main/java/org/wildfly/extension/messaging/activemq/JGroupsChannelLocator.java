package org.wildfly.extension.messaging.activemq;

import java.security.AccessController;
import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;


public class JGroupsChannelLocator {
    /**
     * Callback used by ActiveMQ to locate a JChannel instance corresponding to the channel name
     * passed through the ActiveMQ RA property {@code channelRefName}
     */
    public BroadcastEndpointFactory locateBroadcastEndpointFactory(String channelRefName) throws Exception {
        String[] split = channelRefName.split("/");
        String activeMQServerName = split[0];
        String channelName = split[1];
        @SuppressWarnings("unchecked")
        ServiceController<ActiveMQServer> controller = (ServiceController<ActiveMQServer>) currentServiceContainer().getService(MessagingServices.getActiveMQServiceName(activeMQServerName));
        ActiveMQServerService service = (ActiveMQServerService) controller.getService();
        return service.getBroadcastEndpointFactory(channelName);
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
