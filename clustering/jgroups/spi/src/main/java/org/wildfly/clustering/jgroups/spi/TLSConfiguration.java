/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import javax.net.ssl.SSLContext;

/**
 * Defines the TLS configuration of a JGroups transport protocol.
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
