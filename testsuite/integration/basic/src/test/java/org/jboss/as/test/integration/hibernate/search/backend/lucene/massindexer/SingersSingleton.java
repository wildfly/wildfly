/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.lucene.massindexer;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.orm.Search;

import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import java.util.List;

/**
 * A singleton session bean.
 *
 * @author Hardy Ferentschik
 */
@Singleton
public class SingersSingleton {
    @PersistenceUnit(name = "cmt-test")
    private EntityManagerFactory entityManagerFactory;
    @PersistenceContext(unitName = "cmt-test")
    private EntityManager entityManager;

    public void insertContact(String firstName, String lastName) {
        Singer singer = new Singer();
        singer.setFirstName(firstName);
        singer.setLastName(lastName);
        entityManager.persist(singer);
    }

    public boolean rebuildIndex() {
        try {
            Search.mapping(entityManagerFactory).scope(Object.class).massIndexer()
                    .batchSizeToLoadObjects(30)
                    .threadsToLoadObjects(4)
                    .cacheMode(CacheMode.NORMAL)
                    .startAndWait();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<?> listAllContacts() {
        Query query = entityManager.createQuery("select s from Singer s");
        return query.getResultList();
    }

    public List<?> searchAllContacts() {
        return Search.session(entityManager).search(Singer.class)
                .where(f -> f.matchAll())
                .fetchAllHits();
    }
}


