/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.as.security.elytron;

import static org.wildfly.common.Assert.checkNotNullParam;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A simple {@link Service} that relies on a {@link ValueSupplier} implementation to create an instance of the type that
 * this service provides.
 *
 * @param <T> the type of value that this service provides; may be {@link Void}.
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class BasicService<T> implements Service<T> {

    private ValueSupplier<T> valueSupplier;
    private volatile T value;

    BasicService() {
    }

    BasicService(ValueSupplier<T> valueSupplier) {
        this.valueSupplier = checkNotNullParam("valueSupplier", valueSupplier);
    }

    void setValueSupplier(ValueSupplier<T> valueSupplier) {
        this.valueSupplier = checkNotNullParam("valueSupplier", valueSupplier);
    }

    @Override
    public void start(StartContext context) throws StartException {
        value = checkNotNullParam("valueSupplier", valueSupplier).get();
    }

    @Override
    public void stop(StopContext context) {
        valueSupplier.dispose();
        value = null;
    }

    @Override
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    /**
     * A supplier for the value returned by this service, the {@link #get()} methods allows for a {@link StartException} to be
     * thrown so can be used with failed mandatory service injection.
     */
    @FunctionalInterface
    interface ValueSupplier<T> {

        T get() throws StartException;

        default void dispose() {}
    }
}
