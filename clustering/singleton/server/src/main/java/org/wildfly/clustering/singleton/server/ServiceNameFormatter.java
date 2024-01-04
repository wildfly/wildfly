/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
