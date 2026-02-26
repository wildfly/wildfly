/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATION_TIMEOUT_SECONDS;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.DataSourceDefinition.PATH_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.SUBSYSTEM_NAME;
import static org.jboss.as.connector.subsystems.datasources.XaDataSourceDefinition.PATH_XA_DATASOURCE;

public class DataSourcesTransformers implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_8_0_0 = ModelVersion.create(8, 0, 0);
    private static final ModelVersion VERSION_7_2_0 = ModelVersion.create(7, 2, 0);
    private static final ModelVersion VERSION_7_1_0 = ModelVersion.create(7, 1, 0);
    private static final ModelVersion VERSION_7_0_0 = ModelVersion.create(7, 0, 0);
    private static final ModelVersion EAP_7_4 = ModelVersion.create(6, 0, 0);

    @Override
    public String getSubsystemName() {
        return SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        get720TransformationDescription(chainedBuilder.createBuilder(VERSION_8_0_0, VERSION_7_2_0));

        //no transformation here - just XML parsing change
        chainedBuilder.createBuilder(VERSION_7_2_0, VERSION_7_1_0).build();
        chainedBuilder.createBuilder(VERSION_7_1_0, VERSION_7_0_0).build();

        get600TransformationDescription(chainedBuilder.createBuilder(VERSION_7_0_0, EAP_7_4));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                VERSION_7_2_0, VERSION_7_1_0, VERSION_7_0_0, EAP_7_4
        });
    }

    private static TransformationDescription get720TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        VALIDATION_TIMEOUT_SECONDS
                )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        VALIDATION_TIMEOUT_SECONDS
                )
                .end();
        builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        VALIDATION_TIMEOUT_SECONDS
                )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        VALIDATION_TIMEOUT_SECONDS
                )
                .end();
        return parentBuilder.build();
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