/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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