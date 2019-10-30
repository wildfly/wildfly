/*
 * Copyright 2012 David Blevins
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.adapter;

import org.jboss.as.test.integration.ejb.mdb.dynamic.api.TelnetListener;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetServer;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public class TelnetResourceAdapter implements javax.resource.spi.ResourceAdapter {

    private final Map<Integer, TelnetServer> activated = new HashMap<Integer, TelnetServer>();
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
        final TelnetActivationSpec telnetActivationSpec = (TelnetActivationSpec) activationSpec;

        final TelnetServer telnetServer = activated.remove(port);

        try {
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
