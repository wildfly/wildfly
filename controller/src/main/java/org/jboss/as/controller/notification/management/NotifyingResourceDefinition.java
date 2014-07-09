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

package org.jboss.as.controller.notification.management;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * TODO: temporary workaround for the need to avoid adding methods to ResourceDefinition in EAP 6.x.
 * In WildFly these methods are moved be moved to ResourceDefinition.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 *
 * @deprecated methods will be moved to the parent interface and this interface will be removed in the next major release
 */
@Deprecated
public interface NotifyingResourceDefinition extends ResourceDefinition {

    /**
     * Register notifications associated with this resource.
     *
     * @param resourceRegistration a {@link org.jboss.as.controller.registry.ManagementResourceRegistration} created from this definition
     */
    void registerNotifications(final ManagementResourceRegistration resourceRegistration);

}
