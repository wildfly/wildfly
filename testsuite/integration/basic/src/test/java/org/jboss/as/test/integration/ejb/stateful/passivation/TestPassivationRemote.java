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

package org.jboss.as.test.integration.ejb.stateful.passivation;

/**
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public interface TestPassivationRemote extends AutoCloseable {
    String EXPECTED_RESULT = "true";

    /**
     * Returns the expected result exposed as a static final variable by this interface
     */
    String returnTrueString();

    /**
     * Returns whether or not this instance has been passivated
     */
    boolean hasBeenPassivated();

    /**
     * Returns whether or not this instance has been activated
     */
    boolean hasBeenActivated();

    /**
     * returns true if the beans still share the same XPC
     */
    boolean isPersistenceContextSame();

    void addEntity(int id, String name);

    void removeEntity(int id);

    Employee getSuperEmployee();

    /**
     * returns a value of a property of a CDI bean
     */
    String getManagedBeanMessage();

    void setManagedBeanMessage(String message);

    @Override
    void close();
}
