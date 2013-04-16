package org.jboss.as.undertow.deployment;

import io.undertow.client.HttpClient;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * @author Stuart Douglas
 */
public class WebSocketContainerService implements Service<ServerWebSocketContainer> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WebSocketContainerService");

    private final ServerWebSocketContainer container;
    private final InjectedValue<Pool> injectedBuffer = new InjectedValue<>();
    private final InjectedValue<XnioWorker> xnioWorker = new InjectedValue<>();

    public WebSocketContainerService(final ServerWebSocketContainer container) {
        this.container = container;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        HttpClient client = HttpClient.create(xnioWorker.getValue(), OptionMap.EMPTY);
        container.start(client, injectedBuffer.getValue());
    }

    @Override
    public void stop(final StopContext stopContext) {
        container.stop();
    }

    @Override
    public ServerWebSocketContainer getValue() throws IllegalStateException, IllegalArgumentException {
        return container;
    }

    public InjectedValue<Pool> getInjectedBuffer() {
        return injectedBuffer;
    }

    public InjectedValue<XnioWorker> getXnioWorker() {
        return xnioWorker;
    }
}
