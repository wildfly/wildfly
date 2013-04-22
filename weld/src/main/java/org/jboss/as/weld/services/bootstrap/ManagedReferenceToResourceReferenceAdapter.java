/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.services.bootstrap;

import static org.jboss.weld.util.reflection.Reflections.cast;

import org.jboss.as.naming.ManagedReference;
import org.jboss.weld.injection.spi.ResourceReference;

/**
 * {@link ResourceReference} backed by {@link ManagedReference}.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class ManagedReferenceToResourceReferenceAdapter<T> implements ResourceReference<T> {

    private final ManagedReference reference;

    public ManagedReferenceToResourceReferenceAdapter(ManagedReference reference) {
        this.reference = reference;
    }

    @Override
    public T getInstance() {
        return cast(reference.getInstance());
    }

    @Override
    public void release() {
        reference.release();
    }
}
