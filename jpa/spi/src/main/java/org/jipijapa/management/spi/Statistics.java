/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
