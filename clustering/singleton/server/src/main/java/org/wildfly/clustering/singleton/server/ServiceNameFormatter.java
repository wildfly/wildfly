/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.SimpleFormatter;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;

/**
 * {@link Externalizer} for a {@link ServiceName}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class ServiceNameFormatter extends SimpleFormatter<ServiceName> {

    public ServiceNameFormatter() {
        super(ServiceName.class, ServiceName::parse, ServiceName::getCanonicalName);
    }

    @MetaInfServices(Externalizer.class)
    public static class ServiceNameExternalizer extends StringExternalizer<ServiceName> {
        public ServiceNameExternalizer() {
            super(new ServiceNameFormatter());
        }
    }
}
