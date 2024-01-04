/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyManagedConnectionMetaData implements ManagedConnectionMetaData {
    private static Logger logger = Logger.getLogger(LazyManagedConnectionMetaData.class);

    @Override
    public String getEISProductName() throws ResourceException {
        logger.trace("#LazyManagedConnectionMetaData.getEISProductName");
        return "LAZY";
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        logger.trace("#LazyManagedConnectionMetaData.getEISProductVersion");
        return "1.0";
    }

    @Override
    public int getMaxConnections() throws ResourceException {
        logger.trace("#LazyManagedConnectionMetaData.getMaxConnections");
        return 0;
    }

    @Override
    public String getUserName() throws ResourceException {
        logger.trace("#LazyManagedConnectionMetaData.getUserName");
        return null;
    }
}
