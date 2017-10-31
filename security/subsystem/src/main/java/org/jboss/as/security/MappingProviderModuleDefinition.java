/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * This class should better be called {@code AuditProviderModuleDefinition} rather than {@code MappingProviderModuleDefinition},
 * because it hangs under {@code AuditResourceDefinition} rather than {@code MappingResourceDefinition}.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class MappingProviderModuleDefinition extends MappingModuleDefinition {
    protected static final PathElement PATH_PROVIDER_MODULE = PathElement.pathElement(Constants.PROVIDER_MODULE);
    private static final AttributeDefinition[] ATTRIBUTES = { CODE, MODULE, MODULE_OPTIONS };

    MappingProviderModuleDefinition(String key) {
        super(key);
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }


}
