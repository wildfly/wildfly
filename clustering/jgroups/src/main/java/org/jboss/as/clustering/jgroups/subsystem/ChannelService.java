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
import org.jgroups.protocols.pbcast.STATE;
import org.jgroups.protocols.pbcast.STATE_SOCK;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;

public class ChannelService implements Service<Channel> {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME).append("channel");
    private static final long STATE_TRANSFER_TIMEOUT = 60000;

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
            if (this.channel.getProtocolStack().findProtocol(STATE_TRANSFER.class, STATE.class, STATE_SOCK.class) != null) {
                this.channel.connect(this.id, null, STATE_TRANSFER_TIMEOUT);
            } else {
                this.channel.connect(this.id);
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
        // Validate view
        String localName = this.channel.getName();
        Address localAddress = this.channel.getAddress();
        for (Address address: this.channel.getView()) {
            String name = this.channel.getName(address);
            if ((name != null) && name.equals(localName) && !address.equals(localAddress)) {
                this.channel.close();
                throw JGroupsMessages.MESSAGES.duplicateNodeName(factory.getProtocolStackConfiguration().getEnvironment().getNodeName());
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        if ((this.channel != null) && this.channel.isOpen()) {
            this.channel.close();
        }
    }
}
