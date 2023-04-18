/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
