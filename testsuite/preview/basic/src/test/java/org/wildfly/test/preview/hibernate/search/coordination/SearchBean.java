/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.hibernate.search.coordination;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;

import java.util.List;

@ApplicationScoped
@Transactional
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    public List<String> findAgentNames() {
        return em.createNativeQuery("select name from HSEARCH_AGENT")
                .getResultList();
    }

    public void create(String text) {
        IndexedEntity entity = new IndexedEntity();
        entity.text = text;
        em.persist(entity);
    }

    public List<IndexedEntity> search(String keyword) {
        return Search.session(em).search(IndexedEntity.class)
                .where(f -> f.match().field("text").matching(keyword))
                .fetchAllHits();
    }
}
