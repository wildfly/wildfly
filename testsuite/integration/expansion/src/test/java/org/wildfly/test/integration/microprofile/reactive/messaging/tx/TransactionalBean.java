/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.tx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class TransactionalBean {

    @PersistenceContext(unitName = "test")
    EntityManager em;

    @Transactional
    public void storeValue(String name) {
        ContextEntity entity = new ContextEntity();
        entity.setName(name);
        em.persist(entity);
    }

    @Transactional
    public void checkValues(Set<String> names) {
        checkCount(names.size());

        TypedQuery<ContextEntity> query = em.createQuery("SELECT c from ContextEntity c", ContextEntity.class);
        Set<String> values = query.getResultList().stream().map(v -> v.getName()).collect(Collectors.toSet());
        if (!values.containsAll(names) || !names.containsAll(values)) {
            throw new IllegalStateException("Mismatch of expected names. Expected: " + names + "; actual: " + values);
        }
    }

    @Transactional
    private int checkCount(int expected) {
        TypedQuery<Long> query = em.createQuery("SELECT count(c) from ContextEntity c", Long.class);
        List<Long> result = query.getResultList();
        int count = result.get(0).intValue();
        if (count != expected) {
            throw new IllegalStateException("Expected " + expected + "; got " + count);
        }
        return count;
    }

}
