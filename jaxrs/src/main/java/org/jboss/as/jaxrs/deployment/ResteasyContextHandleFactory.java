/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jaxrs.deployment;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import javax.enterprise.concurrent.ContextService;

import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.resteasy.core.ResteasyContext;

/**
 * A context factory for propagating the RESTEasy context for managed threads.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ResteasyContextHandleFactory implements ContextHandleFactory {
    private static final String NAME = "RESTEASY";
    static ResteasyContextHandleFactory INSTANCE = new ResteasyContextHandleFactory();

    private ResteasyContextHandleFactory() {
    }

    @Override
    public SetupContextHandle saveContext(final ContextService contextService, final Map<String, String> contextObjectProperties) {
        return new ResteasySetupContextHandle();
    }

    @Override
    public int getChainPriority() {
        return 900;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void writeSetupContextHandle(final SetupContextHandle contextHandle, final ObjectOutputStream out) {
    }

    @Override
    public SetupContextHandle readSetupContextHandle(final ObjectInputStream in) {
        return new ResteasySetupContextHandle();
    }

    private static class ResteasySetupContextHandle implements SetupContextHandle {
        private final Map<Class<?>, Object> currentContext;

        private ResteasySetupContextHandle() {
            this.currentContext = ResteasyContext.getContextDataMap();
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            ResteasyContext.pushContextDataMap(currentContext);
            return new ResetContextHandle() {
                @Override
                public void reset() {
                    ResteasyContext.removeContextDataLevel();
                }

                @Override
                public String getFactoryName() {
                    return NAME;
                }
            };
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }
    }
}
