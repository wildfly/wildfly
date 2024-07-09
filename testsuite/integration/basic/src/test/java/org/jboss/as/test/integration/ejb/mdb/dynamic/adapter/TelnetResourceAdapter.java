/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.adapter;

import org.jboss.as.test.integration.ejb.mdb.dynamic.api.TelnetListener;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetServer;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version $Revision$ $Date$
 */
public class TelnetResourceAdapter implements jakarta.resource.spi.ResourceAdapter {

    private final Map<Integer, TelnetServer> activated = new ConcurrentHashMap<>();
    private WorkManager workManager;

    /**
     * Corresponds to the ra.xml <config-property>
     */
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        this.workManager = bootstrapContext.getWorkManager();
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) throws ResourceException {
        final TelnetActivationSpec telnetActivationSpec = (TelnetActivationSpec) activationSpec;

        final MessageEndpoint messageEndpoint = messageEndpointFactory.createEndpoint(null);

        // This messageEndpoint instance is also castable to the ejbClass of the MDB
        final TelnetListener telnetListener = (TelnetListener) messageEndpoint;

        try {
            final TelnetServer telnetServer = new TelnetServer(telnetActivationSpec, telnetListener, port);

            workManager.scheduleWork(new Work() {
                @Override
                public void release() {
                }

                @Override
                public void run() {
                    try {
                        telnetServer.activate();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 0, null, null);
            activated.put(port, telnetServer);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
        final TelnetServer telnetServer = activated.remove(port);

        try {
            Objects.requireNonNull(telnetServer, String.format("No telnetServer found for port %d", port)).deactivate();
            telnetServer.deactivate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final MessageEndpoint endpoint = (MessageEndpoint) telnetServer.getListener();

        endpoint.release();
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return new XAResource[0];
    }

    @Override
    public int hashCode() {
        return port;
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof TelnetResourceAdapter) && ((TelnetResourceAdapter) obj).port == this.port;
    }
}
