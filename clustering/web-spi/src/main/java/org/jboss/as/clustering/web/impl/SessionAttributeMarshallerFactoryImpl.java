/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Default factory for creating session attribute marshallers.
 *
 * @author Paul Ferraro
 */
public class SessionAttributeMarshallerFactoryImpl implements SessionAttributeMarshallerFactory {
    private final MarshallerFactory factory;

    public SessionAttributeMarshallerFactoryImpl() {
        this(Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader()));
    }

    public SessionAttributeMarshallerFactoryImpl(MarshallerFactory factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.SessionAttributeMarshallerFactory#createMarshaller(org.jboss.web.tomcat.service.session.distributedcache.spi.LocalDistributableSessionManager)
     */
    @Override
    public SessionAttributeMarshaller createMarshaller(LocalDistributableSessionManager manager) {
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(manager.getApplicationClassResolver());
        return new SessionAttributeMarshallerImpl(new MarshallingContext(this.factory, configuration));
    }
}
