/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

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
