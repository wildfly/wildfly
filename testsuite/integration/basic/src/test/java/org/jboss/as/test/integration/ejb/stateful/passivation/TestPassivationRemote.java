/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
     * returns a value of a property of a Jakarta Contexts and Dependency Injection bean
     */
    String getManagedBeanMessage();

    void setManagedBeanMessage(String message);

    @Override
    void close();
}
