/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.envers;

import org.hibernate.envers.Audited;

/**
 * Represents a student object.
 *
 * @author Madhumita Sadhukhan
 */
@Audited
public class StudentAudited {
    // unique student id
    private int studentId;
    // first name of the student
    private String firstName;
    // last name of the student
    private String lastName;
    // address of the student
    private String address;

    /**
     * Default constructor
     */
    public StudentAudited() {
    }

    /**
     * Creates a new instance of Student.
     *
     * @param firstName first name.
     * @param lastName  last name.
     * @param address   address.
     */
    public StudentAudited(String firstName, String lastName, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    /**
     * Gets the student id for this student.
     *
     * @return student id.
     */
    public int getStudentId() {
        return studentId;
    }

    /**
     * Sets the student id for this student.
     *
     * @return student id.
     */
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    /**
     * Gets the first name for this student.
     *
     * @return first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name for this student.
     *
     * @param first name.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name for this student.
     *
     * @return last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name for this student.
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
