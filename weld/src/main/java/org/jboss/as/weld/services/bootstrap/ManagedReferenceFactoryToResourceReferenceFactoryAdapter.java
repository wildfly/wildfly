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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * {@link ResourceReferenceFactory} backed by {@link ManagedReferenceFactory}.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class ManagedReferenceFactoryToResourceReferenceFactoryAdapter<T> implements ResourceReferenceFactory<T> {

    private final ManagedReferenceFactory factory;

    public ManagedReferenceFactoryToResourceReferenceFactoryAdapter(ManagedReferenceFactory factory) {
        this.factory = factory;
    }

    @Override
    public ResourceReference<T> createResource() {
        final ManagedReference instance = factory.getReference();
        return new ManagedReferenceToResourceReferenceAdapter<T>(instance);
    }
}
