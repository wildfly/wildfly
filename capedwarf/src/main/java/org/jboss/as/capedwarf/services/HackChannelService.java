package org.jboss.as.capedwarf.services;

import java.lang.reflect.Field;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;

/**
 * Hack handler.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class HackChannelService extends ChannelService {
    public HackChannelService(String id, Value<ChannelFactory> factory) {
        super(id, factory);
    }

    public Channel getValue() {
        try {
            final Field up_handler = Channel.class.getDeclaredField("up_handler");
            up_handler.setAccessible(true);
            final Channel channel = super.getValue();
            up_handler.set(channel, null);
            return channel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
