package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.AsynchronousService;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;
import org.jgroups.protocols.pbcast.STATE;
import org.jgroups.protocols.pbcast.STATE_SOCK;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;

public class ChannelService extends AsynchronousService<Channel> {

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
    protected void start() throws Exception {
        this.channel = this.factory.getValue().createChannel(this.id);
        if (this.channel.getProtocolStack().findProtocol(STATE_TRANSFER.class, STATE.class, STATE_SOCK.class) != null) {
            this.channel.connect(this.id, null, STATE_TRANSFER_TIMEOUT);
        } else {
            this.channel.connect(this.id);
        }
    }

    @Override
    protected void stop() {
        if ((this.channel != null) && this.channel.isOpen()) {
            this.channel.close();
        }
    }
}
