/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.server;

import java.util.function.BiFunction;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.JndiName;

/**
 * @author Paul Ferraro
 */
public enum CacheJndiNameFactory implements BiFunction<String, String, JndiName> {
    REGISTRY_FACTORY("registry"),
    SERVICE_PROVIDER_REGISTRY("providers"),
    ;
    private final String component;

    CacheJndiNameFactory(String component) {
        this.component = component;
    }

    @Override
    public JndiName apply(String containerName, String cacheName) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", this.component, containerName, cacheName);
    }
}
