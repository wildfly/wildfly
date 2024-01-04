/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.naturalid;

/**
 * Represents a Person object.
 *
 * @author Madhumita Sadhukhan
 */
public class Person {
    // unique person id
    private int personId;

    // unique voterid
    private int personVoterId;
    // first name of the person
    private String firstName;
    // last name of the person
    private String lastName;
    // address of the person
    private String address;

    /**
     * Default constructor
     */
    public Person() {
    }

    /**
     * Creates a new instance of Person.
     *
     * @param firstName first name.
     * @param lastName  last name.
     * @param address   address.
     */
    public Person(String firstName, String lastName, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    /**
     * Gets the person id for this student.
     *
     * @return person id.
     */
    public int getPersonId() {
        return personId;
    }

    /**
     * Sets the person id for this Person.
     *
     * @return person id.
     */
    public void setPersonId(int personId) {
        this.personId = personId;
    }

    /**
     * Gets the first name for this person.
     *
     * @return first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name for this person.
     *
     * @param first name.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name for this person.
     *
     * @return last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name for this person.
     *
     * @param last name.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the address for this student.
     *
     * @return address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the address for this student.
     *
     * @param address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    public int getPersonVoterId() {
        return personVoterId;
    }

    public void setPersonVoterId(int personVoterId) {
        this.personVoterId = personVoterId;
    }

    /**
     * Method used by the UI to clear information on the screen.
     *
     * @return String used in the navigation rules.
     */
    public String clear() {
        firstName = "";
        lastName = "";
        address = "";
        return "clear";
    }

}
