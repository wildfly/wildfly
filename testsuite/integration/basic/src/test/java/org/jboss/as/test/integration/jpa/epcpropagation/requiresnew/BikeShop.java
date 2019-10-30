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
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

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
