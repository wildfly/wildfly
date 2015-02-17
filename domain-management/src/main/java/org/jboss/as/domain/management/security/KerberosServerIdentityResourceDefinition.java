/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.KERBEROS;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for a server's Kerberos identity.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class KerberosServerIdentityResourceDefinition extends SimpleResourceDefinition {


    KerberosServerIdentityResourceDefinition() {
        super(PathElement.pathElement(SERVER_IDENTITY, KERBEROS),
                ControllerResolver.getDeprecatedResolver(SecurityRealmResourceDefinition.DEPRECATED_PARENT_CATEGORY, "core.management.security-realm.server-identity.kerberos"),
                new SecurityRealmChildAddHandler(false, false),
                new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        setDeprecated(ModelVersion.create(1, 7));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        // Note: A Kerberos server identity only becomes valid once at least one keytab has been added, however
        //       validation is deferred until a Kerberos authentication definition is added.
        //       This allows Kerberos to be enabled on a realm without forcing it into a batch.
        resourceRegistration.registerSubModel(new KeytabResourceDefinition());
    }

}
