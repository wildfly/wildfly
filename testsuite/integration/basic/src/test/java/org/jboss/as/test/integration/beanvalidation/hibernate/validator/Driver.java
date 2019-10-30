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
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

//import org.hibernate.validator.group.*;

/**
 * @author Madhumita Sadhukhan
 */
// @GroupSequenceProvider(CarGroupSequenceProvider.class)
public class Driver extends UserBean {

    @Min(value = 18, message = "You have to be 18 to drive a car", groups = DriverChecks.class)
    private int age;

    @AssertTrue(message = "You have to pass the driving test and own a valid Driving License", groups = DriverChecks.class)
    public boolean hasDrivingLicense;

    public Driver(String firstName, String lastName) {
        super(firstName, lastName);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean hasValidDrivingLicense() {
        return hasDrivingLicense;
    }

    public void setHasValidDrivingLicense(boolean hasValidDrivingLicense) {
        this.hasDrivingLicense = hasValidDrivingLicense;
    }

}
