/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol;

import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.remoting3.Connection;
import org.xnio.IoFuture;

/**
 * This class is not thread safe and should only be used by one thread
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ProtocolChannelClient implements Closeable {

    private final Configuration configuration;
    private ProtocolChannelClient(final Configuration configuration) {
        this.configuration = configuration;
    }

    public static ProtocolChannelClient create(final Configuration configuration) throws IOException {
        if (configuration == null) {
            throw MESSAGES.nullVar("configuration");
        }
        configuration.validate();
        return new ProtocolChannelClient(configuration);
    }

    public IoFuture<Connection> connect(CallbackHandler handler) throws IOException {
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(handler);
        return ProtocolConnectionUtils.connect(config);
    }

    public IoFuture<Connection> connect(CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext) throws IOException {
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(handler);
        config.setSaslOptions(saslOptions);
        config.setSslContext(sslContext);
        return ProtocolConnectionUtils.connect(config);
    }

    public Connection connectSync(CallbackHandler handler) throws IOException {
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(handler);
        return ProtocolConnectionUtils.connectSync(config);
    }

    public Connection connectSync(CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext) throws IOException {
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(handler);
        config.setSaslOptions(saslOptions);
        config.setSslContext(sslContext);
        return ProtocolConnectionUtils.connectSync(config);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void close() {
        //
    }

    public static final class Configuration extends ProtocolConnectionConfiguration {
        public static int WINDOW_SIZE = ProtocolConnectionConfiguration.DEFAULT_WINDOW_SIZE;

        //Flags to avoid spamming logs with warnings every time someone tries to set these
        private static volatile boolean warnedExecutor;

        public Configuration() {
            super();
        }

        /**
         * @deprecated The executor is no longer needed. Here for backwards compatibility
         */
        @Deprecated
        public void setExecutor(final Executor readExecutor) {
           boolean warned = warnedExecutor;
           if (!warned) {
               warnedExecutor = true;
               ProtocolLogger.CLIENT_LOGGER.executorNotNeeded();
           }
        }
    }

}
