/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
