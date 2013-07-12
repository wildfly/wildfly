package org.jboss.as.remoting;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that publishes socket binding information for remoting connectors
 *
 * @author Stuart Douglas
 */
public class RemotingConnectorBindingInfoService implements Service<RemotingConnectorBindingInfoService.RemotingConnectorInfo> {

    private static final ServiceName SERVICE_NAME = RemotingServices.REMOTING_BASE.append("remotingConnectorInfoService");

    private final RemotingConnectorInfo binding;

    public RemotingConnectorBindingInfoService(RemotingConnectorInfo binding) {
        this.binding = binding;
    }

    public static ServiceName serviceName(final String connectorName) {
        return SERVICE_NAME.append(connectorName);
    }

    public static void install(final ServiceTarget target, final String connectorName, final SocketBinding binding, final String protocol) {
        target.addService(serviceName(connectorName), new RemotingConnectorBindingInfoService(new RemotingConnectorInfo(binding, protocol))).install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public RemotingConnectorInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return binding;
    }

    public static final class RemotingConnectorInfo {
        private final SocketBinding socketBinding;
        private final String protocol;

        public RemotingConnectorInfo(SocketBinding socketBinding, String protocol) {
            this.socketBinding = socketBinding;
            this.protocol = protocol;
        }

        public SocketBinding getSocketBinding() {
            return socketBinding;
        }

        public String getProtocol() {
            return protocol;
        }
    }
}
