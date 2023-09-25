/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
@Stateless
public class BikeManagerBean {

    @EJB
    private BikeRace bikeRace;


    public void runTest() {
        //this enlists the transaction scoped em into the transaction
        Assert.assertTrue(bikeRace.allMotorBikes().isEmpty());

        Motorbike bike = bikeRace.createNewBike(1, "Bike1");
        Assert.assertFalse(bikeRace.contains(bike));


    }

    @EJB
    private BikeShop bikeShop;

    /**
     * When a XPC with pending DB changes should be associated with Jakarta Transactions TX when SFSB enters the SFSB transactional method.
     * as per JPA 7.9.1 Container Responsibilities for XPC:
     * When a business method of the stateful session bean is invoked,
     * if the stateful session bean uses container managed transaction demarcation,
     * and the entity manager is not already associated with the current Jakarta Transactions transaction,
     * the container associates the entity manager with the current Jakarta Transactions transaction and
     * calls EntityManager.joinTransaction.
     */
    public void runTest2() {

        Motorbike bike = bikeShop.downPaymentOnBikeNoTx(2, "Bike2");
        Assert.assertNotNull("newly created bike not be null", bike);
        bikeShop.purchaseNowAndFlushDbChanges();
        Assert.assertNotNull("should be able to find bike in database after purchaseNowAndFlushDbChanges.", bikeShop.find(2));
        try {
            bikeShop.forceRollback(2);
        } catch (RuntimeException ignore) {}
        Assert.assertNotNull("extended persistence context must be associated with Jakarta Transactions tx during " +
                "call to purchaseNowAndFlushDbChanges() and db changes saved when that method ends its Jakarta Transactions tx.", bikeRace.find(2));
    }

}
