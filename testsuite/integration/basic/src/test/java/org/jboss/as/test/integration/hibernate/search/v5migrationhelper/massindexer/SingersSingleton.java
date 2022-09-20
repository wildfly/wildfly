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


