/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.http.server.undertow;

import static org.jboss.as.domain.http.server.undertow.UndertowHttpServerLogger.ROOT_LOGGER;
import io.undertow.server.HttpOpenListener;
import io.undertow.server.HttpTransferEncodingHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.blocking.BlockingHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 * The general HTTP server for handling management API requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UndertowManagementHttpServer {

    private final HttpOpenListener openListener;
    private final InetSocketAddress httpAddress;
    private final InetSocketAddress secureAddress;
    private volatile XnioWorker worker;
    private volatile AcceptingChannel<? extends ConnectedStreamChannel> normalServer;
    private volatile AcceptingChannel<? extends ConnectedStreamChannel> secureServer;
    private final SecurityRealm securityRealm;

    private UndertowManagementHttpServer(HttpOpenListener openListener, InetSocketAddress httpAddress, InetSocketAddress secureAddress, SecurityRealm securityRealm) {
        this.openListener = openListener;
        this.httpAddress = httpAddress;
        this.secureAddress = secureAddress;
        this.securityRealm = securityRealm;
    }


    public void start() {
        final Xnio xnio;
        try {
            //Do what org.jboss.as.remoting.XnioUtil does
            xnio = Xnio.getInstance(null, Module.getModuleFromCallerModuleLoader(ModuleIdentifier.fromString("org.jboss.xnio.nio")).getClassLoader());
        } catch (Exception e) {
            throw new IllegalStateException(e.getLocalizedMessage());
        }
        try {
            //TODO make this configurable
            worker = xnio.createWorker(OptionMap.builder()
                    .set(Options.WORKER_WRITE_THREADS, 4)
                    .set(Options.WORKER_READ_THREADS, 4)
                    .set(Options.CONNECTION_HIGH_WATER, 1000000)
                    .set(Options.CONNECTION_LOW_WATER, 1000000)
                    .set(Options.WORKER_TASK_CORE_THREADS, 10)
                    .set(Options.WORKER_TASK_MAX_THREADS, 12)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.CORK, true)
                    .getMap());

            OptionMap serverOptions = OptionMap.builder()
                    .set(Options.WORKER_ACCEPT_THREADS, 4)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.REUSE_ADDRESSES, true)
                    .getMap();
            ChannelListener<? super AcceptingChannel<ConnectedStreamChannel>> acceptListener = ChannelListeners.openListenerAdapter(openListener);
            if (httpAddress != null) {
                System.out.println("-----> Starting undertow server on " + httpAddress);
                normalServer = worker.createStreamServer(httpAddress, acceptListener, serverOptions);
                normalServer.resumeAccepts();
            }
            if (secureAddress != null) {
                System.out.println("-----> Starting undertow server on " + httpAddress);
                secureServer = worker.createStreamServer(secureAddress, acceptListener, serverOptions);
                secureServer.resumeAccepts();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        System.out.println("-----> Stopping undertow servers");
        IoUtils.safeClose(normalServer);
        IoUtils.safeClose(secureServer);
        worker.shutdown();
    }

    public static UndertowManagementHttpServer create(InetSocketAddress bindAddress, InetSocketAddress secureBindAddress, int backlog,
            ModelControllerClient modelControllerClient, ExecutorService executorService, SecurityRealm securityRealm, ControlledProcessStateService controlledProcessStateService,
            ConsoleMode consoleMode, String consoleSlot)
            throws IOException {

        HttpOpenListener openListener = new HttpOpenListener(new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 8192, 8192 * 8192));
        setupOpenListener(openListener, modelControllerClient, consoleMode, consoleSlot, controlledProcessStateService);
        UndertowManagementHttpServer server = new UndertowManagementHttpServer(openListener, bindAddress, secureBindAddress, securityRealm);



        return server;
    }

    private static void setupOpenListener(HttpOpenListener listener, ModelControllerClient modelControllerClient, ConsoleMode consoleMode, String consoleSlot, ControlledProcessStateService controlledProcessStateService) {
        CanonicalPathHandler canonicalPathHandler = new CanonicalPathHandler();
        HttpTransferEncodingHandler httpTransferEncodingHandler = new HttpTransferEncodingHandler(canonicalPathHandler);
        listener.setRootHandler(httpTransferEncodingHandler);

        PathHandler pathHandler = new PathHandler();
        canonicalPathHandler.setNext(pathHandler);

        ResourceHandler consoleHandler = null;
        try {
            consoleHandler = consoleMode.createConsoleHandler(consoleSlot);
        } catch (ModuleLoadException e) {
            ROOT_LOGGER.consoleModuleNotFound(consoleSlot == null ? "main" : consoleSlot);
        }
        ManagementRootConsoleRedirectHandler rootConsoleRedirectHandler = new ManagementRootConsoleRedirectHandler(consoleHandler);
        DomainApiCheckHandler domainApiHandler = new DomainApiCheckHandler(modelControllerClient, null, controlledProcessStateService);
        pathHandler.setDefaultHandler(rootConsoleRedirectHandler);
        if (consoleHandler != null) {
            pathHandler.addPath(consoleHandler.getContext(), new BlockingHandler(consoleHandler));
        }
        pathHandler.addPath(DomainApiCheckHandler.PATH, domainApiHandler);

    }
}
