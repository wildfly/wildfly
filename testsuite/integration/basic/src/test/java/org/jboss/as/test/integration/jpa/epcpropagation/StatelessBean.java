/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@Local
public class StatelessBean implements StatelessInterface {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createEntity(Integer id, String name) {
        MyEntity entity = em.find(MyEntity.class, id);
        if (entity == null) {
            entity = new MyEntity();
            entity.setId(id);
            em.persist(entity);
        }
        entity.setName(name);
    }

    /**
     * can only be called from a SFSB with an extended persistence context
     *
     * @param id
     * @param name
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void createEntityNoTx(Integer id, String name) {

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String updateEntity(Integer id, String name) {
        MyEntity entity = em.find(MyEntity.class, id);
        String propagatedName = entity.getName();

        entity.setName(name);

        return propagatedName;
    }

}
