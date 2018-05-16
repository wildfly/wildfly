/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate5.infinispan;

import java.util.Map;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;

/**
 * Common implementation class for deprecated region factory implementations.
 * @author Paul Ferraro
 * @author Scott Marlow
 *
 */
class DeprecatedInfinispanRegionFactory extends org.infinispan.hibernate.cache.v53.InfinispanRegionFactory {
    private static final long serialVersionUID = 6795961780643120068L;
    private static final String SHARED = "hibernate.cache.infinispan.shared";
    private final String shared;

    DeprecatedInfinispanRegionFactory(boolean shared) {
        this.shared = Boolean.toString(shared);
    }

    @Override
    public void start(SessionFactoryOptions settings, Map properties) throws CacheException {
        Logger.LOGGER.deprecatedRegionFactory(this.getClass().getName(), this.getClass().getSuperclass().getSuperclass().getName(), SHARED, this.shared);
        properties.put(SHARED, this.shared);
        super.start(settings, properties);
    }
}
