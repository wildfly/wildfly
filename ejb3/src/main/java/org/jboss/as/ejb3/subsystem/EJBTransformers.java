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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ALLOW_EXECUTION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.EXECUTE_IN_WORKER;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REFRESH_INTERVAL;
import static org.jboss.as.ejb3.subsystem.StrictMaxPoolResourceDefinition.DERIVE_SIZE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;

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
        registerTimerTransformers_1_3_0(subsystemRegistration);
        registerTransformers_3_0_0(subsystemRegistration);
        registerTransformers_4_0_0(subsystemRegistration);
    }

    private static void registerTransformers_1_2_1(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_2_1_and_1_3_0(subsystemRegistration, VERSION_1_2_1);
    }

    private static void registerTimerTransformers_1_3_0(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_2_1_and_1_3_0(subsystemRegistration, VERSION_1_3_0);
    }

    private static void registerTransformers_1_2_1_and_1_3_0(SubsystemTransformerRegistration subsystemRegistration, ModelVersion version) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        StatefulCacheRefTransformer statefulCacheRefTransformer = new StatefulCacheRefTransformer();
        builder.setCustomResourceTransformer(statefulCacheRefTransformer);

        for (String name : Arrays.asList(WRITE_ATTRIBUTE_OPERATION, UNDEFINE_ATTRIBUTE_OPERATION, READ_ATTRIBUTE_OPERATION)) {
            builder.addOperationTransformationOverride(name)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(statefulCacheRefTransformer)
                    .end();
        }

        builder.addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(new AddStatefulCacheRefTransformer())
                .end();

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

        registerPassivationStoreTransformers_1_2_1_and_1_3_0(builder);
        registerRemoteTransformers(builder);
        registerMdbDeliveryGroupTransformers(builder);
        registerStrictMaxPoolTransformers(builder);
        registerApplicationSecurityDomainDTransformers(builder);
        registerIdentityTransformers(builder);
        builder.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE));
        if (version.equals(VERSION_1_2_1)) {
            registerTimerTransformers_1_2_0(builder);
        } else if (version.equals(VERSION_1_3_0)) {
            registerTimerTransformers_1_3_0(builder);
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
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME)), CLIENT_MAPPINGS_CLUSTER_NAME)
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

    private static void registerTimerTransformers_1_2_0(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder timerService = parent.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        registerDataStoreTransformers(timerService);
    }

    private static void registerDataStoreTransformers(ResourceTransformationDescriptionBuilder timerService) {

        DataStoreTransformer dataStoreTransformer = new DataStoreTransformer();
        timerService.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, EJB3SubsystemModel.DEFAULT_DATA_STORE)//this is ok, as default-data-store only has any sense with new model, but it is always set!
                .end();
        timerService.discardOperations(ModelDescriptionConstants.ADD);
        timerService.setCustomResourceTransformer(dataStoreTransformer);
        timerService.rejectChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
        ResourceTransformationDescriptionBuilder fileDataStore = timerService.addChildRedirection(EJB3SubsystemModel.FILE_DATA_STORE_PATH, (current, builder) -> builder.getCurrent());

        fileDataStore.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(dataStoreTransformer)
                .end();

    }

    private static void registerTimerTransformers_1_3_0(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder timerService = parent.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        ResourceTransformationDescriptionBuilder db = timerService.addChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
                db.getAttributeBuilder()
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(-1)), REFRESH_INTERVAL)
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(true)), ALLOW_EXECUTION)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, REFRESH_INTERVAL, ALLOW_EXECUTION);
    }

    private static class DataStoreTransformer implements CombinedTransformer {

        private DataStoreTransformer() {
        }

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            Resource original = context.readResourceFromRoot(address);
            String defaultDataStore = original.getModel().get(TimerServiceResourceDefinition.DEFAULT_DATA_STORE.getName()).asString();
            boolean hasFileDataStore = original.hasChild(PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE_PATH.getKey(), defaultDataStore));
            if (original.getChildren(EJB3SubsystemModel.FILE_DATA_STORE).size() > 1 ||
                    !hasFileDataStore) {
                return new TransformedOperation(operation, new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return context.getLogger().getRejectedResourceWarning(address, operation);
                    }
                }, OperationResultTransformer.ORIGINAL_RESULT);
            }
            operation.get(TimerServiceResourceDefinition.THREAD_POOL_NAME.getName()).set(original.getModel().get(TimerServiceResourceDefinition.THREAD_POOL_NAME.getName()));
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            Resource untransformedResource = context.readResource(PathAddress.EMPTY_ADDRESS);
            ModelNode untransformedModel = Resource.Tools.readModel(untransformedResource);
            String defaultDataStore = untransformedModel.get(TimerServiceResourceDefinition.DEFAULT_DATA_STORE.getName()).asString();

            ModelNode transformed = resource.getModel();
            transformed.remove(TimerServiceResourceDefinition.DEFAULT_DATA_STORE.getName());
            ModelNode fileStore = untransformedModel.get(EJB3SubsystemModel.FILE_DATA_STORE, defaultDataStore);
            if (!fileStore.isDefined()) {//happens where default is not file-store
                rejectIncompatibleDataStores(context, address);
            } else if ((untransformedModel.hasDefined(EJB3SubsystemModel.DATABASE_DATA_STORE)
                    && untransformedModel.get(EJB3SubsystemModel.DATABASE_DATA_STORE).keys().size() > 0)
                    || untransformedModel.get(EJB3SubsystemModel.FILE_DATA_STORE).keys().size() > 1) {
                rejectIncompatibleDataStores(context, address);
            }

            ModelNode path = fileStore.get(EJB3SubsystemModel.PATH);
            transformed.get(EJB3SubsystemModel.PATH).set(path);
            transformed.get(EJB3SubsystemModel.RELATIVE_TO).set(fileStore.get(EJB3SubsystemModel.RELATIVE_TO));

            context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            //do not process children!
        }

        private void rejectIncompatibleDataStores(ResourceTransformationContext context, PathAddress address) throws OperationFailedException {
            throw new OperationFailedException(EjbLogger.ROOT_LOGGER.untransformableTimerService(address));
        }
    }

    /*
        * This transformer does the following:
        * - maps <passivation-store/> to <cluster-passivation-store/>
        * - sets appropriate defaults for IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, PASSIVATE_EVENTS_ON_REPLICATE, and CLIENT_MAPPINGS_CACHE
        */
    @SuppressWarnings("deprecation")
    private static void registerPassivationStoreTransformers_1_2_1_and_1_3_0(ResourceTransformationDescriptionBuilder parent) {

        ResourceTransformationDescriptionBuilder child = parent.addChildRedirection(PassivationStoreResourceDefinition.INSTANCE.getPathElement(), PathElement.pathElement(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE));
        child.getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode(true), true), EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("default"), true), EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(Long.valueOf(Integer.MAX_VALUE)), true), EJB3SubsystemModel.IDLE_TIMEOUT)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(TimeUnit.SECONDS.name()), true), EJB3SubsystemModel.IDLE_TIMEOUT_UNIT)
        ;
    }

    /**
     * This Combined Transformer manages this transformation from EAP7 to previous versions (attribute definition/xml markup):
     *
     * DEFAULT_SFSB_CACHE / cache-ref --> DEFAULT_CLUSTERED_SFSB_CACHE / clustered-cache-ref
     * DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE / passivation-disabled-cache-ref --> DEFAULT_SFSB_CACHE / cache-ref
     *
     */
    private static class StatefulCacheRefTransformer implements CombinedTransformer {
        private final Map<String, String> renames;

        public StatefulCacheRefTransformer(){
            renames = new HashMap<>();
            renames.put(DEFAULT_SFSB_CACHE, EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE);
            renames.put(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE, EJB3SubsystemModel.DEFAULT_SFSB_CACHE);
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            if (operation != null && !(operation.hasDefined(OPERATION_HEADERS) && operation.get(OPERATION_HEADERS, "push-to-servers").asBoolean(false)) ){
                String originalAttribute = Operations.getAttributeName(operation);
                if (renames.containsKey(originalAttribute)){
                    operation = operation.clone();
                    operation.get(NAME).set(renames.get(originalAttribute));
                }
            }

            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            Resource untransformedResource = context.readResource(PathAddress.EMPTY_ADDRESS);
            ModelNode untransformedModel = Resource.Tools.readModel(untransformedResource);

            String statefulCache = untransformedModel.get(DEFAULT_SFSB_CACHE).asString();
            String statefulPassivationDisabledCache = untransformedModel.get(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE).asString();

            ModelNode transformed = resource.getModel();
            transformed.get(DEFAULT_SFSB_CACHE).set(statefulPassivationDisabledCache);
            transformed.get(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE).set(statefulCache);
            transformed.remove(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE);

            final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            childContext.processChildren(resource);
        }
    }

    private static class AddStatefulCacheRefTransformer implements OperationTransformer{

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            if (operation != null && !(operation.hasDefined(OPERATION_HEADERS) && operation.get(OPERATION_HEADERS, "push-to-servers").asBoolean(false)) ){
                operation = operation.clone();

                String statefulCache = operation.get(DEFAULT_SFSB_CACHE).asString();
                String statefulPassivationDisabledCache = operation.get(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE).asString();

                operation.get(DEFAULT_SFSB_CACHE).set(statefulPassivationDisabledCache);
                operation.get(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE).set(statefulCache);
                operation.remove(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE);
            }
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

}
