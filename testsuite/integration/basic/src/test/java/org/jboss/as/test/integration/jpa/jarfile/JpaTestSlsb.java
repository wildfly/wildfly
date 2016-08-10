/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jpa.jarfile;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.metamodel.EntityType;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
@Stateless
public class JpaTestSlsb {

    @PersistenceUnit
    private EntityManagerFactory emf;

    @PersistenceContext
    private EntityManager em;

    public void testMainArchiveEntity() {
        final EntityType<MainArchiveEntity> meta = emf.getMetamodel().entity(MainArchiveEntity.class);
        Assert.assertNotNull("class must be an entity", meta);
        MainArchiveEntity entity = new MainArchiveEntity();
        entity.setId(1);
        entity.setName("Bob");
        entity.setAddress("123 Fake St");
        em.persist(entity);
        em.flush();
    }

    public void testJarFileEntity() {
        final EntityType<JarFileEntity> meta = emf.getMetamodel().entity(JarFileEntity.class);
        Assert.assertNotNull("class must be an entity", meta);
        JarFileEntity entity = new JarFileEntity();
        entity.setId(1);
        entity.setName("Bob");
        entity.setAddress("123 Fake St");
        em.persist(entity);
        em.flush();
    }
}
