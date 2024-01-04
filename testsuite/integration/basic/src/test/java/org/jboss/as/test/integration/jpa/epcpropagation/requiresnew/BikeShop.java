/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * SFSB with REQUIRES_NEW transaction attribute. Even though a transaction scoped
 * em has been enrolled in the outer transaction, this bean should still work.
 *
 * @author Stuart Douglas
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class BikeShop {


    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    protected EntityManager entityManager;

    public Motorbike createMotorBike(int id, String name) {
        Motorbike bike = new Motorbike(id, name);
        entityManager.persist(bike);
        entityManager.flush();
        return bike;
    }

    /**
     * create new motor bike but don't save to the DB until purchaseNow is invoked.
     *
     * @param id
     * @param name
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Motorbike downPaymentOnBikeNoTx(int id, String name) {
        Motorbike bike = new Motorbike(id, name);
        entityManager.persist(bike);
        return bike;
    }

    /**
     * save MotorBikes that we previously put a down payment on
     * pending DB changes should be flushed as per JPA 7.9.1 Container Responsibilities for XPC:
     * When a business method of the stateful session bean is invoked,
     * if the stateful session bean uses container managed transaction demarcation,
     * and the entity manager is not already associated with the current JTA transaction,
     * the container associates the entity manager with the current JTA transaction and
     * calls EntityManager.joinTransaction.
     */
    public void purchaseNowAndFlushDbChanges() {

    }

    public void forceRollback(int id) {
        // purchaseNow should of saved the MotorBike, so we should be able to find the MotorBike in the db
        Motorbike bike = entityManager.find(Motorbike.class, id);
        throw new RuntimeException("for ejb container to rollback tx");
    }

    public Motorbike find(int id) {
        Motorbike bike = entityManager.find(Motorbike.class, id);
        return bike;
    }
}
