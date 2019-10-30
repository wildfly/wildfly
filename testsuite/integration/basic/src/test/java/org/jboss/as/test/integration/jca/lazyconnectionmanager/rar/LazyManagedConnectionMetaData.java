/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

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
