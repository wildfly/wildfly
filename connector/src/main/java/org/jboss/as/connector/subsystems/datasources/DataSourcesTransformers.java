/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.DataSourceDefinition.PATH_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.SUBSYSTEM_NAME;
import static org.jboss.as.connector.subsystems.datasources.XaDataSourceDefinition.PATH_XA_DATASOURCE;

public class DataSourcesTransformers implements ExtensionTransformerRegistration {

    private static final ModelVersion EAP_7_4 = ModelVersion.create(6, 0, 0);

    @Override
    public String getSubsystemName() {
        return SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        get600TransformationDescription(chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EAP_7_4));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                EAP_7_4
        });
    }

    private static TransformationDescription get600TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        EXCEPTION_SORTER_MODULE,
                        STALE_CONNECTION_CHECKER_MODULE,
                        VALID_CONNECTION_CHECKER_MODULE
                )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        EXCEPTION_SORTER_MODULE,
                        STALE_CONNECTION_CHECKER_MODULE,
                        VALID_CONNECTION_CHECKER_MODULE
                )
                .end();
        builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        EXCEPTION_SORTER_MODULE,
                        STALE_CONNECTION_CHECKER_MODULE,
                        VALID_CONNECTION_CHECKER_MODULE
                )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        EXCEPTION_SORTER_MODULE,
                        STALE_CONNECTION_CHECKER_MODULE,
                        VALID_CONNECTION_CHECKER_MODULE
                )
                .end();
        return parentBuilder.build();
    }

}