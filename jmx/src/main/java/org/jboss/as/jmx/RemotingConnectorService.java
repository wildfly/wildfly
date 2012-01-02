package org.jboss.as.jmx;

import java.io.IOException;

import javax.management.MBeanServer;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.remoting.management.ManagementRemotingServices;
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
import org.jboss.remoting3.jmx.RemotingConnectorServer;

/**
 * The remote connector services
 *
 * @author Stuart Douglas
 */
public class RemotingConnectorService implements Service<RemotingConnectorServer> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jmx", "remoting-connector-ref");

    private RemotingConnectorServer server;

    private final InjectedValue<MBeanServer> mBeanServer = new InjectedValue<MBeanServer>();

    private final InjectedValue<Endpoint> endpoint = new InjectedValue<Endpoint>();

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        server = new RemotingConnectorServer(mBeanServer.getValue(), endpoint.getValue());
        try {
            server.start();
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        try {
            server.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized RemotingConnectorServer getValue() throws IllegalStateException, IllegalArgumentException {
        return server;
    }

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceVerificationHandler verificationHandler) {

        final RemotingConnectorService service = new RemotingConnectorService();
        final ServiceBuilder<RemotingConnectorServer> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.mBeanServer);
        builder.addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpoint);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        return builder.install();
    }


}
