/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Formatter;

/**
 * {@link Externalizer} for a {@link ServiceName}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class ServiceNameFormatter extends Formatter.Provided<ServiceName> {

    public ServiceNameFormatter() {
        super(Formatter.Identity.INSTANCE.wrap(ServiceName.class, ServiceName::getCanonicalName, ServiceName::parse));
    }
}
