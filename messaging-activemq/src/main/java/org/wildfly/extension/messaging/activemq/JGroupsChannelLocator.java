package org.wildfly.extension.messaging.activemq;

import java.security.AccessController;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jgroups.JChannel;


public class JGroupsChannelLocator {
    /**
     * Callback used by ActiveMQ to locate a JChannel instance corresponding to the channel name
     * passed through the ActiveMQ RA property {@code channelRefName}
     */
    public JChannel locateChannel(String channelRefName) {
        String[] split = channelRefName.split("/");
        String activeMQServerName = split[0];
        String channelName = split[1];
        ServiceController<ActiveMQServer> controller = (ServiceController<ActiveMQServer>) currentServiceContainer().getService(MessagingServices.getActiveMQServiceName(activeMQServerName));
        ActiveMQServerService service = (ActiveMQServerService) controller.getService();
        return service.getChannels().get(channelName);
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
