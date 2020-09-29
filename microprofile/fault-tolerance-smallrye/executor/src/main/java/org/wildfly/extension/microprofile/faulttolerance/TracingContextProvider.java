/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.faulttolerance;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 *
 * @author Ladislav Thon (c) 2020 Red Hat, Inc.
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
final class TracingContextProvider implements MiniContextPropagation.ContextProvider {
    private final Tracer tracer;

    TracingContextProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public MiniContextPropagation.ContextSnapshot capture() {
        ScopeManager scopeManager = tracer.scopeManager();
        Scope activeScope = scopeManager.active();

        if (activeScope != null) {
            Span span = activeScope.span();
            return () -> {
                Scope propagated = scopeManager.activate(span, false);
                return propagated::close;
            };
        }

        return MiniContextPropagation.ContextSnapshot.NOOP;
    }
}
