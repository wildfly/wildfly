/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.jpa2lc;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author Jan Martiska
 */
@Stateless
@LocalBean
@Path("/")
public class DummyEntityRESTResource {

    @PersistenceContext
    private EntityManager em;

    @GET
    @Path("/create/{id}")
    public void createNew(@PathParam(value = "id") Long id) {
        final DummyEntity entity = new DummyEntity();
        entity.setId(id);
        em.persist(entity);
    }

    @GET
    @Path("/cache/{id}")
    public void addToCacheByQuerying(@PathParam(value = "id") Long id) {
        em.createQuery("select b from DummyEntity b where b.id=:id", DummyEntity.class)
                .setParameter("id", id)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.USE)
                .setHint("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS)
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
