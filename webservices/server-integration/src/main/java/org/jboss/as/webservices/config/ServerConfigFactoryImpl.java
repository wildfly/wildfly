/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;

import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;

/**
 * Retrieves webservices stack specific config.
 *
 * @author <a href="mailto:hbraun@redhat.com">Heiko Braun</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServerConfigFactoryImpl extends ServerConfigFactory {

    private static volatile ServerConfig config;

    public ServerConfig getServerConfig() {
        return config;
    }

    public static void setConfig(final ServerConfig config) {
        ServerConfigFactoryImpl.config = config;
    }

    public static ServerConfig getConfig() {
        return ServerConfigFactoryImpl.config;
    }

}
