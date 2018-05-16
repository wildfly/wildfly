/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.GET_INSTALLED_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.INSTALLED_DRIVERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.INSTALLED_DRIVERS_LIST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stefano Maestri
 */
public class DataSourcesSubsystemRootDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME);
    private final boolean registerRuntimeOnly;
    private final boolean deployed;


    private DataSourcesSubsystemRootDefinition(final boolean registerRuntimeOnly, final boolean deployed) {
        super(new Parameters(PATH_SUBSYSTEM, DataSourcesExtension.getResourceDescriptionResolver())
                .setAddHandler(deployed ? null : DataSourcesSubsystemAdd.INSTANCE)
                .setRemoveHandler(deployed ? null : ReloadRequiredRemoveStepHandler.INSTANCE)
                .setFeature(!deployed));
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
    }

    public static DataSourcesSubsystemRootDefinition createInstance(final boolean registerRuntimeOnly) {
        return new DataSourcesSubsystemRootDefinition(registerRuntimeOnly, false);
    }

    public static DataSourcesSubsystemRootDefinition createDeployedInstance(final boolean registerRuntimeOnly) {
            return new DataSourcesSubsystemRootDefinition(registerRuntimeOnly, true);
        }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        if (registerRuntimeOnly && ! deployed) {
            resourceRegistration.registerOperationHandler(INSTALLED_DRIVERS_LIST, InstalledDriversListOperationHandler.INSTANCE);
            resourceRegistration.registerOperationHandler(GET_INSTALLED_DRIVER, GetInstalledDriverOperationHandler.INSTANCE);
        }

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (registerRuntimeOnly && ! deployed )
            resourceRegistration.registerReadOnlyAttribute(INSTALLED_DRIVERS, InstalledDriversListOperationHandler.INSTANCE);

    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (! deployed )
            resourceRegistration.registerSubModel(JdbcDriverDefinition.INSTANCE);
        resourceRegistration.registerSubModel(DataSourceDefinition.createInstance(registerRuntimeOnly, deployed));
        resourceRegistration.registerSubModel(XaDataSourceDefinition.createInstance(registerRuntimeOnly, deployed));
    }
}
