package org.jboss.as.capedwarf.services;

import java.lang.reflect.Field;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.protocols.pbcast.STATE;
import org.jgroups.protocols.pbcast.STATE_SOCK;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;

/**
 * Hack handler.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class HackChannelService extends AsynchronousService<Channel> {
    private static final long STATE_TRANSFER_TIMEOUT = 60000;

    private final InjectedValue<ChannelFactory> factory = new InjectedValue<ChannelFactory>();
    private final String id;
    private volatile Channel channel;

    public HackChannelService(String id) {
        this.id = id;
    }

    private void hackHandler() throws Exception {
        final Field up_handler = Channel.class.getDeclaredField("up_handler");
        up_handler.setAccessible(true);
        up_handler.set(channel, null);
    }

    protected void start() throws Exception {
        ChannelFactory factory = this.factory.getValue();
        this.channel = factory.createChannel(this.id);
        // hack mux handler
        hackHandler();
        // set multi receiver
        // channel.setReceiver(new ClassloadingMultiJGroupsMasterMessageListener());
        // connect
        if (this.channel.getProtocolStack().findProtocol(STATE_TRANSFER.class, STATE.class, STATE_SOCK.class) != null) {
            this.channel.connect(this.id, null, STATE_TRANSFER_TIMEOUT);
        } else {
            this.channel.connect(this.id);
        }
        // Validate view
        String localName = this.channel.getName();
        Address localAddress = this.channel.getAddress();
        for (Address address : this.channel.getView()) {
            String name = this.channel.getName(address);
            if (name.equals(localName) && !address.equals(localAddress)) {
                this.channel.close();
                throw JGroupsMessages.MESSAGES.duplicateNodeName(factory.getServerEnvironment().getNodeName());
            }
        }
    }

    protected void stop() {
        if ((this.channel != null) && this.channel.isOpen()) {
            this.channel.close();
        }
    }

    public Channel getValue() {
        return channel;
    }

    public InjectedValue<ChannelFactory> getFactory() {
        return factory;
    }
}
