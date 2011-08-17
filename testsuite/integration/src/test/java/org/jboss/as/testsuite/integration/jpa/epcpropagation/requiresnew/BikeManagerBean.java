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
package org.jboss.as.testsuite.integration.jpa.epcpropagation.requiresnew;

import org.junit.Assert;

import javax.ejb.EJB;
import javax.ejb.Stateless;

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

}
