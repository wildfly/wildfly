package org.jboss.as.messaging;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jgroups.JChannel;


public class JGroupsChannelLocator {
    /**
     * Callback used by HornetQ to locate a JChannel instance corresponding to the channel name
     * passed through the HornetQ RA property {@code channelRefName}
     */
    public JChannel locateChannel(String channelRefName) {
        String[] split = channelRefName.split("/");
        String hornetQServerName = split[0];
        String channelName = split[1];
        ServiceController<HornetQServer> controller = (ServiceController<HornetQServer>) CurrentServiceContainer.getServiceContainer().getService(MessagingServices.getHornetQServiceName(hornetQServerName));
        HornetQService service = (HornetQService) controller.getService();
        return service.getChannels().get(channelName);
    }
}
