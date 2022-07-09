/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search.cdi.beans.model;

import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class EntityWithCDIAwareBridgesDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void create(EntityWithCDIAwareBridges entity) {
        entityManager.persist(entity);
    }

    @Transactional
    public void update(EntityWithCDIAwareBridges entity) {
        entityManager.merge(entity);
    }

    @Transactional
    public void delete(EntityWithCDIAwareBridges entity) {
        entity = entityManager.merge(entity);
        entityManager.remove(entity);
    }

    @Transactional
    public void deleteAll() {
        CriteriaDelete<EntityWithCDIAwareBridges> delete = entityManager.getCriteriaBuilder()
                .createCriteriaDelete(EntityWithCDIAwareBridges.class);
        delete.from(EntityWithCDIAwareBridges.class);
        entityManager.createQuery(delete).executeUpdate();
        Search.session(entityManager).workspace(EntityWithCDIAwareBridges.class).purge();
    }

    @Transactional
    public List<Long> searchValueBridge(String terms) {
        return Search.session(entityManager).search(EntityWithCDIAwareBridges.class)
                .select(f -> f.id(Long.class))
                .where(f -> f.match().fields("value_fr", "value_en", "value_de")
                        .matching(terms, ValueConvert.NO))
                .fetchAllHits();
    }

    @Transactional
    public List<Long> searchTypeBridge(String terms) {
        return Search.session(entityManager).search(EntityWithCDIAwareBridges.class)
                .select(f -> f.id(Long.class))
                .where(f -> f.match().field(EntityWithCDIAwareBridges.TYPE_BRIDGE_FIELD_NAME)
                        .matching(terms, ValueConvert.NO))
                .fetchAllHits();
    }
}