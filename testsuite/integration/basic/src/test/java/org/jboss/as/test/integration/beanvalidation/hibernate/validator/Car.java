/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.validator.group.GroupSequenceProvider;

/**
 * @author Madhumita Sadhukhan
 */
@GroupSequenceProvider(CarGroupSequenceProvider.class)
public class Car {

    private String model;

    @NotBlank
    private String number;

    @NotEmpty
    @Valid
    private List<UserBean> passengers = new ArrayList<UserBean>();

    @AssertTrue(message = "The rent for the Car has to be paid before being driven", groups = FinalInspection.class)
    public boolean hasBeenPaid;

    @Min(value = 4, message = "Car should have minimum 4 seats", groups = FinalInspection.class)
    public int seats;

    @Valid
    private Driver driver;

    @AssertTrue(message = "The Car has to pass the fuel test and inspection test before being driven", groups = CarChecks.class)
    public boolean passedvehicleInspection;

    public Car(String number, List<UserBean> passengers) {

        this.number = number;
        this.passengers = passengers;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<UserBean> getPassengers() {

        return passengers;

    }

    public void setPassengers(List<UserBean> passengers) {
        this.passengers = passengers;
    }

    public boolean inspectionCompleted() {
        return passedvehicleInspection;

    }

    public void setPassedVehicleInspection(boolean passedvehicleInspection) {
        this.passedvehicleInspection = passedvehicleInspection;
    }

    public void setHasBeenPaid(boolean hasBeenPaid) {
        this.hasBeenPaid = hasBeenPaid;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

}
