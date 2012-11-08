package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;

public class ChannelService implements Service<Channel>, ChannelListener {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME).append("channel");

    public static ServiceName getServiceName(String id) {
        return SERVICE_NAME.append(id);
    }

    private final Value<ChannelFactory> factory;
    private final String id;
    private volatile Channel channel;

    public ChannelService(String id, Value<ChannelFactory> factory) {
        this.id = id;
        this.factory = factory;
    }

    @Override
    public Channel getValue() {
        return this.channel;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ChannelFactory factory = this.factory.getValue();
        try {
            this.channel = factory.createChannel(this.id);
            this.channel.addChannelListener(this);
            // Don't connect the channel here
            // This will be done by Infinispan (see AS7-5904)
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.channel != null) {
            this.channel.removeChannelListener(this);
        }
        this.channel = null;
    }

    @Override
    public void channelClosed(Channel channel) {
        // Do nothing
    }

    @Override
    public void channelConnected(Channel channel) {
        // Validate view
        String localName = channel.getName();
        Address localAddress = channel.getAddress();
        for (Address address: channel.getView()) {
            String name = channel.getName(address);
            if ((name != null) && name.equals(localName) && !address.equals(localAddress)) {
                channel.close();
                throw JGroupsMessages.MESSAGES.duplicateNodeName(this.factory.getValue().getProtocolStackConfiguration().getEnvironment().getNodeName());
            }
        }
    }

    @Override
    public void channelDisconnected(Channel channel) {
        // Do nothing
    }
}
