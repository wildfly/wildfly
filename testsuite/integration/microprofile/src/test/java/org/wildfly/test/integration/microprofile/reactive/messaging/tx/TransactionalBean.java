/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.tx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;


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
