/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
class MappingProviderModuleDefinition extends MappingModuleDefinition {
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
