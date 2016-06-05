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
package org.jboss.as.clustering.naming;

import org.jboss.as.naming.deployment.JndiName;

/**
 * Factory methods for creating a JndiName.
 * @author Paul Ferraro
 */
public class JndiNameFactory {
    public static final String DEFAULT_JNDI_NAMESPACE = "java:jboss";
    public static final String DEFAULT_LOCAL_NAME = "default";

    public static JndiName parse(String value) {
        return value.startsWith("java:") ? JndiName.of(value) : createJndiName(DEFAULT_JNDI_NAMESPACE, value.startsWith("/") ? value.substring(1) : value);
    }

    public static JndiName createJndiName(String namespace, String... contexts) {
        JndiName name = JndiName.of(namespace);
        for (String context: contexts) {
            name = name.append((context != null) ? context : DEFAULT_LOCAL_NAME);
        }
        return name;
    }

    private JndiNameFactory() {
        // Hide
    }
}
