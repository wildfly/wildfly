/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
