/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextService;

import java.util.Map;

/**
 * The EE10 ContextHandleFactory, which should replace the legacy one once all impls are migrated.
 * @author emmartins
 */
public interface EE10ContextHandleFactory extends ContextHandleFactory {

    /**
     *
     * @return the context type the factory provides handles for
     */
    String getContextType();

    /**
     * @param contextService
     * @param contextObjectProperties
     * @return a SetupContextHandle which partially or fully clears the factory's context type
     */
    SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties);

    /**
     * @param contextService
     * @param contextObjectProperties
     * @return a SetupContextHandle which partially or fully propagates the factory's context type
     */
    SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties);

    /**
     * @param contextService
     * @param contextObjectProperties
     * @return a SetupContextHandle which partially or fully unchanges the factory's context type
     */
    default SetupContextHandle unchangedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return null;
    }

    @Override
    default SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        throw new UnsupportedOperationException();
    }
}
