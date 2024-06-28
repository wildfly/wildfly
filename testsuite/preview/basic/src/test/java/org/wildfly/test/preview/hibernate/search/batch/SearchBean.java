/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.hibernate.search.batch;

import java.util.List;

import org.hibernate.search.mapper.orm.Search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    public void create(int n) {
        for (int i = 0; i < n; i++) {
            IndexedEntity entity = new IndexedEntity();
            entity.text = "text " + i;
            em.persist(entity);
        }
    }

    public List<IndexedEntity> search(String keyword) {
        return Search.session(em).search(IndexedEntity.class).where(f -> f.match().field("text").matching(keyword))
                .fetchAllHits();
    }
}
