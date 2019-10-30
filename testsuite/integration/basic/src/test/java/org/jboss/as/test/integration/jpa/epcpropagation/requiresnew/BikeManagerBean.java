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

import javax.ejb.EJB;
import javax.ejb.Stateless;

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
     * When a XPC with pending DB changes should be associated with JTA TX when SFSB enters the SFSB transactional method.
     * as per JPA 7.9.1 Container Responsibilities for XPC:
     * When a business method of the stateful session bean is invoked,
     * if the stateful session bean uses container managed transaction demarcation,
     * and the entity manager is not already associated with the current JTA transaction,
     * the container associates the entity manager with the current JTA transaction and
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
        Assert.assertNotNull("extended persistence context must be associated with JTA tx during " +
                "call to purchaseNowAndFlushDbChanges() and db changes saved when that method ends its JTA tx.", bikeRace.find(2));
    }

}
