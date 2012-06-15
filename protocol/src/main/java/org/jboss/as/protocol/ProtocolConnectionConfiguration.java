/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
public class ProtocolConnectionConfiguration {

    public static final int DEFAULT_WINDOW_SIZE = 0x8000;

    private static final long DEFAULT_CONNECT_TIMEOUT = 5000;

    private URI uri;
    private Endpoint endpoint;
    private OptionMap optionMap = OptionMap.EMPTY;
    private long connectionTimeout = DEFAULT_CONNECT_TIMEOUT;
    private CallbackHandler callbackHandler;
    private Map<String, String> saslOptions = Collections.emptyMap();
    private SSLContext sslContext;

    protected ProtocolConnectionConfiguration() {
        //
    }

    protected void validate() {
        if (endpoint == null) {
            throw MESSAGES.nullVar("endpoint");
        }
        if (optionMap == null) {
            throw MESSAGES.nullVar("optionMap");
        }
        if (uri == null) {
            throw MESSAGES.nullVar("uri");
        }
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public OptionMap getOptionMap() {
        return optionMap;
    }

    public void setOptionMap(OptionMap optionMap) {
        this.optionMap = optionMap;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public Map<String, String> getSaslOptions() {
        return saslOptions;
    }

    public void setSaslOptions(Map<String, String> saslOptions) {
        this.saslOptions = saslOptions;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public static ProtocolConnectionConfiguration create(final Endpoint endpoint, final URI uri) {
        return create(endpoint, uri, OptionMap.EMPTY);
    }

    public static ProtocolConnectionConfiguration create(final Endpoint endpoint, final URI uri, final OptionMap options) {
        final ProtocolConnectionConfiguration configuration = new ProtocolConnectionConfiguration();
        configuration.setEndpoint(endpoint);
        configuration.setUri(uri);
        configuration.setOptionMap(options);
        return configuration;
    }

    public static ProtocolConnectionConfiguration copy(final ProtocolConnectionConfiguration old) {
        ProtocolConnectionConfiguration configuration = new ProtocolConnectionConfiguration();
        configuration.uri = old.uri;
        configuration.endpoint = old.endpoint;
        configuration.optionMap = old.optionMap;
        configuration.connectionTimeout = old.connectionTimeout;
        configuration.callbackHandler = old.callbackHandler;
        configuration.saslOptions = old.saslOptions;
        configuration.sslContext = old.sslContext;
        return configuration;
    }

}
