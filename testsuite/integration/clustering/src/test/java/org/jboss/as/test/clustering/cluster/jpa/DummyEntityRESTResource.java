/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * @author Jan Martiska
 */
@RequestScoped
@Path("/")
public class DummyEntityRESTResource {

    @PersistenceContext(unitName = "MainPU")
    private EntityManager em;

    @Inject
    private UserTransaction tx;

    @GET
    @Path("/create/{id}")
    public void createNew(@PathParam(value = "id") Long id) throws Exception {
        final DummyEntity entity = new DummyEntity();
        entity.setId(id);
        tx.begin();
        em.persist(entity);
        tx.commit();
    }

    @GET
    @Path("/cache/{id}")
    public void addToCacheByQuerying(@PathParam(value = "id") Long id) {
        em.createQuery("select b from DummyEntity b where b.id=:id", DummyEntity.class)
                .setParameter("id", id)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.USE)
                .setHint("jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS)
                .getSingleResult();
    }

    @GET
    @Path("/evict/{id}")
    public void evict(@PathParam(value = "id") Long id) {
        em.getEntityManagerFactory().getCache().evict(DummyEntity.class, id);
    }

    @GET
    @Path("/isInCache/{id}")
    @Produces("text/plain")
    public boolean isEntityInCache(@PathParam(value = "id") Long id) {
        return em.getEntityManagerFactory().getCache().contains(DummyEntity.class, id);
    }

}
