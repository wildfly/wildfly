/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.deployment;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Configuration object used to managed a collection a JNDI namespace bindings for a deployment.  This is primarily used
 * to detect duplicate namespace bindings.
 *
 * @author John E. Bailey
 */
public class NamespaceBindings {
    public static final AttachmentKey<NamespaceBindings> ATTACHMENT_KEY = AttachmentKey.create(NamespaceBindings.class);
    private final ConcurrentMap<JndiName, Object> bindings = new ConcurrentHashMap<JndiName, Object>();

    /**
     * Get the namespace bindings for a deployment context.
     *
     * @param context The deployment context
     * @return The existing namespace bindings, or a new instance if they don't already exist.
     */
    public static NamespaceBindings getNamespaceBindings(final DeploymentUnit context) {
        NamespaceBindings namespaceBindings = context.getAttachment(ATTACHMENT_KEY);
        if(namespaceBindings == null) {
            namespaceBindings = new NamespaceBindings();
            context.putAttachment(ATTACHMENT_KEY, namespaceBindings);
        }
        return namespaceBindings;
    }

    /**
     * Add a new namespace binding for this deployment.  This will return a boolean that can be used to determine if this
     * is the authoritative binding (the first), and should actually be performed.
     *
     * @param name The JNDI name of the binding
     * @param value The value of the binding
     * @return true if this is the authoritative binding
     * @throws DuplicateBindingException If this binding already exists and is not compatible with the existing binding.
     */
    public boolean addBinding(final JndiName name, final Object value) throws DuplicateBindingException {
        final Object existing = bindings.putIfAbsent(name, value);
        if (existing != null && !existing.equals(value)) {
            throw new DuplicateBindingException(NamingLogger.ROOT_LOGGER.duplicateBinding(name, existing, value));
        }
        return existing == null;
    }
}
