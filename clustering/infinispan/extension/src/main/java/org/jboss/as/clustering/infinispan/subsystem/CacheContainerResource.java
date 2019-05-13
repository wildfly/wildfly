/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collections;
import java.util.Map;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;

/**
 * @author Paul Ferraro
 */
public class CacheContainerResource extends ComplexResource implements Registrar<String> {

    private static final String CHILD_TYPE = CacheRuntimeResourceDefinition.WILDCARD_PATH.getKey();

    public CacheContainerResource(Resource resource) {
        this(resource, Collections.singletonMap(CHILD_TYPE, new CacheRuntimeResourceProvider()));
    }

    private CacheContainerResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, CacheContainerResource::new);
    }

    @Override
    public Registration register(String cache) {
        ChildResourceProvider provider = this.apply(CHILD_TYPE);
        provider.getChildren().add(cache);
        return new Registration() {
            @Override
            public void close() {
                provider.getChildren().remove(cache);
            }
        };
    }
}
