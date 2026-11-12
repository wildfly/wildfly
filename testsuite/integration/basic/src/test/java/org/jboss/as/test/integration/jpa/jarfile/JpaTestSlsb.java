/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.jarfile;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.metamodel.EntityType;

import org.junit.jupiter.api.Assertions;

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
        Assertions.assertNotNull(meta, "class must be an entity");
        MainArchiveEntity entity = new MainArchiveEntity();
        entity.setId(1);
        entity.setName("Bob");
        entity.setAddress("123 Fake St");
        em.persist(entity);
        em.flush();
    }

    public void testJarFileEntity() {
        final EntityType<JarFileEntity> meta = emf.getMetamodel().entity(JarFileEntity.class);
        Assertions.assertNotNull(meta, "class must be an entity");
        JarFileEntity entity = new JarFileEntity();
        entity.setId(1);
        entity.setName("Bob");
        entity.setAddress("123 Fake St");
        em.persist(entity);
        em.flush();
    }
}
