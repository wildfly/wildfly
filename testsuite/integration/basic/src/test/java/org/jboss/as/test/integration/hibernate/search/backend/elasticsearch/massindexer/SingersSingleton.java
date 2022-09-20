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
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.massindexer;

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


