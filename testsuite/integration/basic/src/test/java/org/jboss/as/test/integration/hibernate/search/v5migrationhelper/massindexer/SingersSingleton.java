/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.v5migrationhelper.massindexer;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;

import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * A singleton session bean.
 *
 * @author Hardy Ferentschik
 */
@Singleton
// We know the migration helper is deprecated; we want to test it anyway.
@SuppressWarnings("deprecation")
public class SingersSingleton {
    @PersistenceContext(unitName = "cmt-test")
    private EntityManager entityManager;

    public void insertContact(String firstName, String lastName) {
        Singer singer = new Singer();
        singer.setFirstName(firstName);
        singer.setLastName(lastName);
        entityManager.persist(singer);
    }

    public boolean rebuildIndex() throws InterruptedException {
        FullTextEntityManager fullTextEntityManager = Search
                .getFullTextEntityManager(entityManager);
        try {
            fullTextEntityManager
                    .createIndexer()
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
        FullTextEntityManager fullTextEntityManager = Search
                .getFullTextEntityManager(entityManager);

        FullTextQuery query = fullTextEntityManager.createFullTextQuery(
                new MatchAllDocsQuery(),
                Singer.class
        );

        return query.getResultList();
    }
}


