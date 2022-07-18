/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.ChildResourceProvider;
import org.jboss.as.clustering.controller.ComplexResource;
import org.jboss.as.clustering.controller.SimpleChildResourceProvider;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.ejb3.timerservice.spi.TimerListener;

/**
 * Dynamic management resource for a timer service.
 * @author baranowb
 * @author Paul Ferraro
 */
public class TimerServiceResource extends ComplexResource implements TimerListener {

    private static final String CHILD_TYPE = EJB3SubsystemModel.TIMER_PATH.getKey();

    public TimerServiceResource() {
        this(PlaceholderResource.INSTANCE, Collections.singletonMap(CHILD_TYPE, new SimpleChildResourceProvider(ConcurrentHashMap.newKeySet())));
    }

    private TimerServiceResource(Resource resource, Map<String, ChildResourceProvider> providers) {
        super(resource, providers, TimerServiceResource::new);
    }

    @Override
    public void timerAdded(String id) {
        ChildResourceProvider handler = this.apply(CHILD_TYPE);
        handler.getChildren().add(id);
    }

    @Override
    public void timerRemoved(String id) {
        ChildResourceProvider handler = this.apply(CHILD_TYPE);
        handler.getChildren().remove(id);
    }
}
