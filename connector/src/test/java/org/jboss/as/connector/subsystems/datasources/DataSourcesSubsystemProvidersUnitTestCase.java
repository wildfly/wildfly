/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import org.junit.Test;

/**
 * Tests of {@link DataSourcesSubsystemProviders}.
 *
 * @author Brian Stansberry
 */
public class DataSourcesSubsystemProvidersUnitTestCase {

    @Test
    public void testAddJdbcDriverDesc() {
        DataSourcesSubsystemProviders.ADD_JDBC_DRIVER_DESC.getModelDescription(null);
    }

    @Test
    public void testDescribeJdbcDriverDesc() {
        DataSourcesSubsystemProviders.DESCRIBE_JDBC_DRIVER_DESC.getModelDescription(null);
    }

    @Test
    public void testJdbcDriverDesc() {
        DataSourcesSubsystemProviders.JDBC_DRIVER_DESC.getModelDescription(null);
    }

    @Test
    public void testRemoveJdbcDriverDesc() {
        DataSourcesSubsystemProviders.REMOVE_JDBC_DRIVER_DESC.getModelDescription(null);
    }

    @Test
    public void testSubsystem() {
        DataSourcesSubsystemProviders.SUBSYSTEM.getModelDescription(null);
    }

    @Test
    public void testSubsystemAddDesc() {
        DataSourcesSubsystemProviders.SUBSYSTEM_ADD_DESC.getModelDescription(null);
    }
}
