/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.ejb3.rar;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Connector(
   reauthenticationSupport = false,
   transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction)
public class SimpleQueueResourceAdapter implements ResourceAdapter {
    private static final Logger log = Logger.getLogger(SimpleQueueResourceAdapter.class);

    private static final BlockingQueue<String> queue = new LinkedBlockingDeque<String>();
    private static WorkManager workManager;
    private static List<MessageEndpointFactory> endpointFactories = new LinkedList<MessageEndpointFactory>();

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        if (workManager != null)
            throw new ResourceAdapterInternalException("Can only start once");
        workManager = ctx.getWorkManager();
    }

    @Override
    public void stop() {
        workManager = null;
    }

    public static void deliver(String message) throws WorkException {
        queue.add(message);
        workManager.doWork(new Work() {
            @Override
            public void release() {
                Thread.currentThread().interrupt();
            }

            @Override
            public void run() {
                try {
                    process();
                } catch (UnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        endpointFactories.add(endpointFactory);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        endpointFactories.remove(endpointFactory);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        // no crash recovery
        return null;
    }

    private static void process() throws UnavailableException {
        if (endpointFactories.size() == 0)
            return;
        MessageEndpoint endpoint = endpointFactories.get(0).createEndpoint(null);
        try {
            while (!queue.isEmpty()) {
                try {
                    String message = queue.poll(30, SECONDS);
                    try {
                        ((PostmanPat) endpoint).deliver(message);
                    } catch (Throwable t) {
                        log.error("Failed to deliver message " + message, t);
                        // ignore
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        } finally {
            endpoint.release();
        }
    }
}
