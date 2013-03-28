package org.jboss.as.test.integration.respawn;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.client.impl.AbstractModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolConnectionManager;
import org.jboss.as.protocol.mgmt.FutureManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

/**
 * @author Emanuel Muckenhuber
 */
class TestControllerClient extends AbstractModelControllerClient {

    private final ManagementChannelHandler channelHandler;
    private final ProtocolConnectionManager connectionManager;
    private final ChannelStrategy channelStrategy = new ChannelStrategy();

    TestControllerClient(final ProtocolConnectionConfiguration configuration, final ExecutorService executor) {
        connectionManager = ProtocolConnectionManager.create(configuration, channelStrategy);
        channelHandler = new ManagementChannelHandler(channelStrategy, executor, this);
    }

    @Override
    protected ManagementChannelAssociation getChannelAssociation() throws IOException {
        return channelHandler;
    }

    protected void connect() throws IOException {
        connectionManager.connect();
    }

    protected ModelNode executeAwaitClosed(final ModelNode operation) throws IOException {
        final Channel channel = getChannelAssociation().getChannel();
        final Connection connection = channel.getConnection();
        final ModelNode result = execute(operation);
        if(! ModelDescriptionConstants.SUCCESS.equals(result.get(ModelDescriptionConstants.OUTCOME).asString())) {
            return result;
        }
        try {
            connection.awaitClosed();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            try {
                channelHandler.shutdown();
            } finally {
                IoUtils.safeClose(channelStrategy);
            }
        } finally {
            channelHandler.shutdownNow();
        }
    }

    private class ChannelStrategy extends FutureManagementChannel {

        @Override
        public void connectionOpened(final Connection connection) throws IOException {
            final Channel channel = openChannel(connection, "management", OptionMap.EMPTY);
            if(setChannel(channel)) {
                channel.receiveMessage(channelHandler.getReceiver());
                channel.addCloseHandler(channelHandler);
            } else {
                channel.closeAsync();
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connectionManager.shutdown();
            }
        }
    }

}
