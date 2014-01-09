package org.jboss.as.remoting;

import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.UnknownURISchemeException;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.spi.ExternalConnectionProvider;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.AssembledConnectedSslStreamChannel;
import org.xnio.channels.AssembledConnectedStreamChannel;
import org.xnio.ssl.SslConnection;

/**
 * Service that registers a HTTP upgrade handler to enable remoting to be used via http upgrade.
 *
 * @author Stuart Douglas
 */
public class RemotingHttpUpgradeService implements Service<RemotingHttpUpgradeService> {


    public static final String JBOSS_REMOTING = "jboss-remoting";

    /**
     * Magic number used in the handshake.
     * <p/>
     * The handshake borrows heavily from the web socket protocol, but uses different header
     * names and a different magic number.
     */
    public static final String MAGIC_NUMBER = "CF70DEB8-70F9-4FBA-8B4F-DFC3E723B4CD";

    //headers
    public static final String SEC_JBOSS_REMOTING_KEY = "Sec-JbossRemoting-Key";
    public static final String SEC_JBOSS_REMOTING_ACCEPT = "Sec-JbossRemoting-Accept";

    /**
     * Base service name for this HTTP Upgrade refist
     */
    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName UPGRADE_SERVICE_NAME = ServiceName.JBOSS.append("remoting ", "remoting-http-upgrade-service");

    private final String httpConnectorName;
    private final String endpointName;


    private final InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private final InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();
    private final InjectedValue<Endpoint> injectedEndpoint = new InjectedValue<>();
    private final InjectedValue<RemotingSecurityProvider> securityProviderValue = new InjectedValue<>();
    private final OptionMap connectorPropertiesOptionMap;

    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public RemotingHttpUpgradeService(final String httpConnectorName, final String endpointName, final OptionMap connectorPropertiesOptionMap) {
        this.httpConnectorName = httpConnectorName;
        this.endpointName = endpointName;
        this.connectorPropertiesOptionMap = connectorPropertiesOptionMap;
    }


    public static void installServices(final ServiceTarget serviceTarget, final String remotingConnectorName, final String httpConnectorName, final ServiceName endpointName, final OptionMap connectorPropertiesOptionMap, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {
        final RemotingHttpUpgradeService service = new RemotingHttpUpgradeService(httpConnectorName, endpointName.getSimpleName(), connectorPropertiesOptionMap);

        final ServiceName securityProviderName = RealmSecurityProviderService.createName(remotingConnectorName);

        ServiceBuilder<RemotingHttpUpgradeService> builder = serviceTarget.addService(UPGRADE_SERVICE_NAME.append(remotingConnectorName), service)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .addDependency(HTTP_UPGRADE_REGISTRY.append(httpConnectorName), ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.listenerRegistry)
                .addDependency(endpointName, Endpoint.class, service.injectedEndpoint)
                .addDependency(securityProviderName, RemotingSecurityProvider.class, service.securityProviderValue);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        ServiceController<RemotingHttpUpgradeService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }


    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Endpoint endpoint = injectedEndpoint.getValue();
        RemotingSecurityProvider rsp = securityProviderValue.getValue();
        ServerAuthenticationProvider sap = rsp.getServerAuthenticationProvider();
        OptionMap.Builder builder = OptionMap.builder();
        builder.addAll(rsp.getOptionMap());

        ListenerRegistry.Listener listenerInfo = listenerRegistry.getValue().getListener(httpConnectorName);
        assert listenerInfo != null;
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata("jboss-remoting", endpointName));
        RemotingConnectorBindingInfoService.install(context.getChildTarget(), context.getController().getName().getSimpleName(), (SocketBinding)listenerInfo.getContextInformation("socket-binding"), listenerInfo.getProtocol().equals("https") ? "https-remoting" : "http-remoting");

        if (connectorPropertiesOptionMap != null) {
            builder.addAll(connectorPropertiesOptionMap);
        }
        OptionMap resultingMap = builder.getMap();
        try {
            final ExternalConnectionProvider provider = endpoint.getConnectionProviderInterface(Protocol.HTTP_REMOTING.toString(), ExternalConnectionProvider.class);
            final ExternalConnectionProvider.ConnectionAdaptor adaptor = provider.createConnectionAdaptor(resultingMap, sap);

            injectedRegistry.getValue().addProtocol(JBOSS_REMOTING, new ChannelListener<StreamConnection>() {
                @Override
                public void handleEvent(final StreamConnection channel) {
                    if (channel instanceof SslConnection) {
                        adaptor.adapt(new AssembledConnectedSslStreamChannel((SslConnection) channel, channel.getSourceChannel(), channel.getSinkChannel()));
                    } else {
                        adaptor.adapt(new AssembledConnectedStreamChannel(channel, channel.getSourceChannel(), channel.getSinkChannel()));
                    }
                }
            }, new SimpleHttpUpgradeHandshake(MAGIC_NUMBER, SEC_JBOSS_REMOTING_KEY, SEC_JBOSS_REMOTING_ACCEPT));

        } catch (UnknownURISchemeException e) {
            throw new StartException(e);
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        listenerRegistry.getValue().getListener(httpConnectorName).removeHttpUpgradeMetadata(httpUpgradeMetadata);
        httpUpgradeMetadata = null;
        injectedRegistry.getValue().removeProtocol(JBOSS_REMOTING);
    }

    @Override
    public synchronized RemotingHttpUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
