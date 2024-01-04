/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.management.spi;

import java.util.Collection;
import java.util.Set;

/**
 * SPI for statistic plugins to implement.
 *
 * @author Scott Marlow
 */
public interface Statistics {


    /**
     * Get the statistics names
     *
     * @return The value
     */
    Set<String> getNames();

    Collection<String> getDynamicChildrenNames(EntityManagerFactoryAccess entityManagerFactoryAccess, PathAddress pathAddress);

    /**
     * Get the type
     *
     * @param name of the statistic
     * @return The value
     */
    Class getType(String name);

    /**
     * return true if the specified name represents an operation.
     *
     * @param name of the statistic
     * @return
     */
    boolean isOperation(String name);

    /**
     * return true if the specified name represents an attribute.
     *
     * @param name of the statistic
     * @return
     */
    boolean isAttribute(String name);

    /**
     * return true if the specified name represents a writeable attribute
     * @param name of the statistics
     * @return
     */
    boolean isWriteable(String name);

    /**
     * for loading descriptions of statistics/operations
     *
     * @return name of resource bundle name
     */
    String getResourceBundleName();

    /**
     * gets the key prefix for referencing descriptions of statistics/operations
     *
     * @return
     */
    String getResourceBundleKeyPrefix();

    /**
     * Get the value of the statistics
     *
     * @param name The name of the statistics
     * @return The value
     */
    Object getValue(String name, EntityManagerFactoryAccess entityManagerFactoryAccess, StatisticName statisticName, PathAddress pathAddress);

    /**
     * Set the value of the statistic (isWriteable must return true)
     * @param name
     * @param newValue
     */
    void setValue(String name, Object newValue, EntityManagerFactoryAccess entityManagerFactoryAccess, StatisticName statisticName, PathAddress pathAddress);

    /**
     * get the names of the children statistic levels (if any)
     * @return set of names
     */
    Set<String> getChildrenNames();

    /**
     * get the specified children statistics
     * @param childName name of the statistics to return
     * @return
     */
    Statistics getChild(String childName);


}
