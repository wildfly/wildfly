/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.EXECUTE_IN_WORKER;
import static org.jboss.as.ejb3.subsystem.StrictMaxPoolResourceDefinition.DERIVE_SIZE;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class EJBTransformers implements ExtensionTransformerRegistration {
    private static final ModelVersion VERSION_1_2_1 = ModelVersion.create(1, 2, 1);
    private static final ModelVersion VERSION_1_3_0 = ModelVersion.create(1, 3, 0);
    private static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);
    private static final ModelVersion VERSION_4_0_0 = ModelVersion.create(4, 0, 0);

    @Override
    public String getSubsystemName() {
        return EJB3Extension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_2_1(subsystemRegistration);
        registerTransformers_1_3_0(subsystemRegistration);
        registerTransformers_3_0_0(subsystemRegistration);
        registerTransformers_4_0_0(subsystemRegistration);
    }

    private static void registerTransformers_1_2_1(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_2_1_and_1_3_0(subsystemRegistration, VERSION_1_2_1);
    }

    private static void registerTransformers_1_3_0(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_2_1_and_1_3_0(subsystemRegistration, VERSION_1_3_0);
    }

    private static void registerTransformers_1_2_1_and_1_3_0(SubsystemTransformerRegistration subsystemRegistration, ModelVersion version) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().addRename(DEFAULT_SFSB_CACHE, EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE);
        builder.getAttributeBuilder().addRename(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE, DEFAULT_SFSB_CACHE);
        //This used to behave as 'true' and it is now defaulting as 'true'
        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(true)), EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS);

        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS);
        // We can always discard this attribute, because it's meaningless without the security-manager subsystem, and
        // a legacy slave can't have that subsystem in its profile.
        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS);
        //builder.getAttributeBuilder().setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("hornetq-ra"), true), EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME);


        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);

        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);

        PassivationStoreResourceDefinition.registerTransformers_1_2_1_and_1_3_0(builder);
        registerRemoteTransformers(builder);
        registerMdbDeliveryGroupTransformers(builder);
        registerStrictMaxPoolTransformers(builder);
        registerApplicationSecurityDomainDTransformers(builder);
        registerIdentityTransformers(builder);
        builder.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE));
        if (version.equals(VERSION_1_2_1)) {
            TimerServiceResourceDefinition.registerTransformers_1_2_0(builder);
        } else if (version.equals(VERSION_1_3_0)) {
            TimerServiceResourceDefinition.registerTransformers_1_3_0(builder);
        }

        // Rename new statistics-enabled attribute to old enable-statistics
        builder.getAttributeBuilder()
               .addRename(EJB3SubsystemModel.STATISTICS_ENABLED, EJB3SubsystemModel.ENABLE_STATISTICS);

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, version);
    }

    private static void registerTransformers_3_0_0(SubsystemTransformerRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("hornetq-ra"), true), EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME)
                .end();
        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);
        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);
        registerMdbDeliveryGroupTransformers(builder);
        registerRemoteTransformers(builder);
        registerStrictMaxPoolTransformers(builder);
        registerApplicationSecurityDomainDTransformers(builder);
        registerIdentityTransformers(builder);

        // Rename new statistics-enabled attribute to old enable-statistics
        builder.getAttributeBuilder().addRename(EJB3SubsystemModel.STATISTICS_ENABLED, EJB3SubsystemModel.ENABLE_STATISTICS);
        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, VERSION_3_0_0);
    }

    private static void registerTransformers_4_0_0(SubsystemTransformerRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        registerApplicationSecurityDomainDTransformers(builder);
        registerIdentityTransformers(builder);
        builder.addChildResource(RemotingProfileResourceDefinition.INSTANCE).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, StaticEJBDiscoveryDefinition.INSTANCE)
                .end();

        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);

        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);

        // Rename new statistics-enabled attribute to old enable-statistics
        builder.getAttributeBuilder()
               .addRename(EJB3SubsystemModel.STATISTICS_ENABLED, EJB3SubsystemModel.ENABLE_STATISTICS);

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, VERSION_4_0_0);
    }

    private static void registerRemoteTransformers(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder remoteService = parent.addChildResource(EJB3SubsystemModel.REMOTE_SERVICE_PATH);
        remoteService.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME)), CLIENT_MAPPINGS_CLUSTER_NAME)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_MAPPINGS_CLUSTER_NAME)
                .setDiscard(DiscardAttributeChecker.ALWAYS, EXECUTE_IN_WORKER) //as this does not affect functionality we just discard
                .end();
    }

    private  static void registerIdentityTransformers(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(EJB3SubsystemModel.IDENTITY_PATH);
    }

    private  static void registerApplicationSecurityDomainDTransformers(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN));
    }

    private static void registerStrictMaxPoolTransformers(ResourceTransformationDescriptionBuilder parent) {
        parent.addChildResource(PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(StrictMaxPoolResourceDefinition.DeriveSize.NONE.toString())), DERIVE_SIZE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, DERIVE_SIZE);
    }

    private static void registerMdbDeliveryGroupTransformers(ResourceTransformationDescriptionBuilder parent) {
        parent.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP));
    }

}
