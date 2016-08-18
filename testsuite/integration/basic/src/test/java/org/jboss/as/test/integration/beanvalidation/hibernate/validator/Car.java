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

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
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
