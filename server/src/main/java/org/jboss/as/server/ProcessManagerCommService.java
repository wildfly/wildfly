/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.as.process.AbstractProcessMessageHandler;
import org.jboss.as.process.ProcessManagerClient;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerCommService implements Service<Void> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.pmcomm");

    private final byte[] authKey;
    private final ProtocolClient.Configuration configuration;
    private final InjectedValue<ScheduledExecutorService> executor = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ProcessManagerClient> client = new InjectedValue<ProcessManagerClient>();

    private StopContext stopContext;
    private boolean stop;

    public ProcessManagerCommService(final byte[] authKey, final ProtocolClient.Configuration configuration) {
        this.authKey = authKey;
        this.configuration = configuration;
    }

    private final Runnable reconnectTask = new Runnable() {
        public void run() {
            try {
                final ProcessManagerClient newClient = ProcessManagerClient.connect(configuration, authKey, new AbstractProcessMessageHandler() {
                    public void handleConnectionShutdown(final ProcessManagerClient client) {
                        StreamUtils.safeClose(client);
                    }

                    public void handleConnectionFailure(final ProcessManagerClient client, final IOException cause) {
                        log.warnf("Process manager connection failed: %s", cause);
                        StreamUtils.safeClose(client);
                    }

                    public void handleConnectionFinished(final ProcessManagerClient client) {
                        synchronized (ProcessManagerCommService.this) {
                            if (stopContext != null) {
                                stopContext.complete();
                                stopContext = null;
                                ProcessManagerCommService.this.client.uninject();
                            } else if (stop) {
                                ProcessManagerCommService.this.client.uninject();
                            } else {
                                executor.getValue().execute(reconnectTask);
                            }
                        }
                    }
                });
                client.inject(newClient);
            } catch (IOException e) {
                log.warnf("Failed to connect to process manager; retry in 30 seconds: %s", e);
                executor.getValue().schedule(this, 30, TimeUnit.SECONDS);
            }
        }
    };

    public synchronized void start(final StartContext context) throws StartException {
        client.inject(null);
        executor.getValue().execute(reconnectTask);
    }

    public synchronized void stop(final StopContext context) {
        final ProcessManagerClient value = client.getValue();
        stop = true;
        if (value != null) {
            context.asynchronous();
            stopContext = context;
            StreamUtils.safeClose(value);
            client.uninject();
        }
    }

    public Void getValue() throws IllegalStateException {
        return null;
    }

    ProcessManagerClient getClient() {
        return client.getValue();
    }

    public Injector<ScheduledExecutorService> getExecutorInjector() {
        return executor;
    }
}
