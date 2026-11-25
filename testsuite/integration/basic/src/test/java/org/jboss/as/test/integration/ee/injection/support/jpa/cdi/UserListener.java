/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class UserListener {

    @Inject
    private UserCollector collector;

    @PrePersist
    public void prePersist(final User entity) {
        collector.push(EventType.PRE_PERSIST, entity);
    }

    @PreRemove
    public void preRemove(final User entity) {
        collector.push(EventType.PRE_REMOVE, entity);
    }

    @PreUpdate
    public void preUpdate(final User entity) {
        collector.push(EventType.PRE_UPDATE, entity);
    }

    @PostLoad
    public void postLoad(final User entity) {
        collector.push(EventType.POST_LOAD, entity);
    }

    @PostPersist
    public void postPersist(final User entity) {
        collector.push(EventType.POST_PERSIST, entity);
    }

    @PostRemove
    public void postRemove(final User entity) {
        collector.push(EventType.POST_REMOVE, entity);
    }

    @PostUpdate
    public void postUpdate(final User entity) {
        collector.push(EventType.POST_UPDATE, entity);
    }
}
