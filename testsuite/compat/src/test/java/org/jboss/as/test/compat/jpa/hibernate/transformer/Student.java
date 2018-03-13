/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.compat.jpa.hibernate.transformer;

/**
 * Represents a student object.
 *
 * @author Madhumita Sadhukhan
 */
public class Student {
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
    public Student() {
    }

    /**
     * Creates a new instance of Student.
     *
     * @param firstName first name.
     * @param lastName  last name.
     * @param address   address.
     */
    public Student(String firstName, String lastName, String address) {
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
