package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;
import org.jgroups.protocols.pbcast.STATE;
import org.jgroups.protocols.pbcast.STATE_SOCK;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;

public class ChannelService implements Service<Channel> {

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
    public Channel getValue() throws IllegalStateException, IllegalArgumentException {
        return this.channel;
    }

    @Override
    public void start(StartContext arg0) throws StartException {
        try {
            this.channel = this.factory.getValue().createChannel(this.id);
            if (this.channel.getProtocolStack().findProtocol(STATE_TRANSFER.class, STATE.class, STATE_SOCK.class) != null) {
                this.channel.connect(this.id, null, 0);
            } else {
                this.channel.connect(this.id);
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext arg0) {
        this.channel.close();
        this.channel = null;
    }
}
