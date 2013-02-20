package org.jboss.as.undertow;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.undertow.ajp.AjpOpenListener;
import io.undertow.server.OpenListener;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class AJPListenerService extends AbstractListenerService<AJPListenerService> {

    private AcceptingChannel<? extends ConnectedStreamChannel> server;

    public AJPListenerService(String name) {
        super(name);
    }

    @Override
    protected OpenListener createOpenListener() {
        return new AjpOpenListener(getBufferPool().getValue(), getBufferSize());
    }

    @Override
    void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<? super AcceptingChannel<ConnectedStreamChannel>> acceptListener) throws IOException {
        server = worker.createStreamServer(binding.getValue().getSocketAddress(), acceptListener, SERVER_OPTIONS);
        server.resumeAccepts();
        UndertowLogger.ROOT_LOGGER.listenerStarted("AJP", getName(), binding.getValue().getSocketAddress());
    }

    @Override
    void stopListening() {
        server.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("AJP", getName());
        IoUtils.safeClose(server);
        UndertowLogger.ROOT_LOGGER.listenerStopped("AJP", getName(), getBinding().getValue().getSocketAddress());
    }

    @Override
    public AJPListenerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public boolean isSecure() {
        return false;
    }
}
