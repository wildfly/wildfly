/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import javax.net.ssl.SSLContext;

/**
 * Defines the configuration of a TLS-secured socket transport protocol, like TCP and TCP_NIO2.
 *
 * @author Radoslav Husar
 */
public interface TLSConfiguration {

    /**
     * Returns the client {@link SSLContext}.
     */
    SSLContext getClientSSLContext();

    /**
     * Returns the server {@link SSLContext}.
     */
    SSLContext getServerSSLContext();
}
