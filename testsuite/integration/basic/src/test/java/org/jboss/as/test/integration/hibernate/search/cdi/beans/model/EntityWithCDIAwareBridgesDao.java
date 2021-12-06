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

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaDelete;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
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
        Search.getFullTextEntityManager(entityManager).purgeAll(EntityWithCDIAwareBridges.class);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Long> searchFieldBridge(String terms) {
        FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(EntityWithCDIAwareBridges.class).get();
        Query luceneQuery = queryBuilder.keyword()
                .onField("internationalizedValue")
                .ignoreFieldBridge()
                .matching(terms)
                .createQuery();
        FullTextQuery query = ftEntityManager.createFullTextQuery(luceneQuery, EntityWithCDIAwareBridges.class);
        query.setProjection(ProjectionConstants.ID);
        List<Object[]> projections = query.getResultList();
        return projections.stream()
                .map(array -> (Long) array[0])
                .collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Long> searchClassBridge(String terms) {
        FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = ftEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(EntityWithCDIAwareBridges.class).get();
        Query luceneQuery = queryBuilder.keyword()
                .onField(EntityWithCDIAwareBridges.CLASS_BRIDGE_FIELD_NAME)
                .ignoreFieldBridge()
                .matching(terms)
                .createQuery();
        FullTextQuery query = ftEntityManager.createFullTextQuery(luceneQuery, EntityWithCDIAwareBridges.class);
        query.setProjection(ProjectionConstants.ID);
        List<Object[]> projections = query.getResultList();
        return projections.stream()
                .map(array -> (Long) array[0])
                .collect(Collectors.toList());
    }
}