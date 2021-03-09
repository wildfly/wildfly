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
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_1_2_1;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_1_3_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_3_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_4_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_5_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_6_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_7_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_8_0_0;
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
import org.jboss.as.clustering.controller.transform.RejectNonSingletonListAttributeChecker;
import org.jboss.as.clustering.controller.transform.SingletonListAttributeConverter;
import org.jboss.as.clustering.controller.transform.DiscardSingletonListAttributeChecker;
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
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.threads.PoolAttributeDefinitions;
import org.jboss.dmr.ModelNode;

/**
 * Jakarta Enterprise Beans Transformers used to transform current model version to legacy model versions for domain mode.
 *
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 * @author Richard Achmatowicz (c) 2020 Red Hat Inc.
 */
public class EJBTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return EJB3Extension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        registerTransformers_8_0_0(chainedBuilder.createBuilder(currentModel, VERSION_8_0_0.getVersion()));
        registerTransformers_7_0_0(chainedBuilder.createBuilder(VERSION_8_0_0.getVersion(), VERSION_7_0_0.getVersion()));
        registerTransformers_6_0_0(chainedBuilder.createBuilder(VERSION_7_0_0.getVersion(), VERSION_6_0_0.getVersion()));
        registerTransformers_5_0_0(chainedBuilder.createBuilder(VERSION_6_0_0.getVersion(), VERSION_5_0_0.getVersion()));
        registerTransformers_4_0_0(chainedBuilder.createBuilder(VERSION_5_0_0.getVersion(), VERSION_4_0_0.getVersion()));
        registerTransformers_3_0_0(chainedBuilder.createBuilder(VERSION_4_0_0.getVersion(), VERSION_3_0_0.getVersion()));
        registerTransformers_1_3_0(chainedBuilder.createBuilder(VERSION_3_0_0.getVersion(), VERSION_1_3_0.getVersion()));
        registerTransformers_1_2_1(chainedBuilder.createBuilder(VERSION_1_3_0.getVersion(), VERSION_1_2_1.getVersion()));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                VERSION_8_0_0.getVersion(), VERSION_7_0_0.getVersion(), VERSION_6_0_0.getVersion(), VERSION_5_0_0.getVersion(),
                VERSION_4_0_0.getVersion(), VERSION_3_0_0.getVersion(), VERSION_1_3_0.getVersion(), VERSION_1_2_1.getVersion()});
    }

    /*
     * Transformers for changes in model version 1.3.0
     */
    private static void registerTransformers_1_2_1(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        DataStoreTransformer dataStoreTransformer = new DataStoreTransformer();

        ResourceTransformationDescriptionBuilder timerService = subsystemBuilder.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        timerService.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, EJB3SubsystemModel.DEFAULT_DATA_STORE)//this is ok, as default-data-store only has any sense with new model, but it is always set!
                .end();
        timerService.discardOperations(ModelDescriptionConstants.ADD);

        // set a custom resource transformer for /subsystem=ejb/service=timer
        timerService.setCustomResourceTransformer(dataStoreTransformer);

        // reject /subsystem=ejb/service=timer database-data-store children
        timerService.rejectChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);

        ResourceTransformationDescriptionBuilder fileDataStore = timerService.addChildRedirection(EJB3SubsystemModel.FILE_DATA_STORE_PATH, (current, theBuilder) -> theBuilder.getCurrent());

        // override the operation /subsystem=ejb/service=timer/file-data-store=F:add(path=, relative-to=)
        fileDataStore.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(dataStoreTransformer)
                .end();
    }

    /*
     * Transformers for changes in model version 3.0.0
     */
    private static void registerTransformers_1_3_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        StatefulCacheRefTransformer statefulCacheRefTransformer = new StatefulCacheRefTransformer();
        subsystemBuilder.setCustomResourceTransformer(statefulCacheRefTransformer);
        for (String name : Arrays.asList(WRITE_ATTRIBUTE_OPERATION, UNDEFINE_ATTRIBUTE_OPERATION, READ_ATTRIBUTE_OPERATION)) {
            subsystemBuilder.addOperationTransformationOverride(name)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(statefulCacheRefTransformer)
                    .end();
        }
        subsystemBuilder.addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(new AddStatefulCacheRefTransformer())
                .end();

        //This used to behave as 'true' and it is now defaulting as 'true'
        subsystemBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS)
                .end();

        // We can always discard this attribute, because it's meaningless without the security-manager subsystem, and a legacy slave can't have that subsystem in its profile.
        subsystemBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ModelNode.FALSE), EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                .end();

        subsystemBuilder.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE));

        // passivation store transformers
        subsystemBuilder.addChildRedirection(PassivationStoreResourceDefinition.INSTANCE.getPathElement(), PathElement.pathElement(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE))
                .getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(ModelNode.TRUE, true), EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("default"), true), EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(Long.valueOf(Integer.MAX_VALUE)), true), EJB3SubsystemModel.IDLE_TIMEOUT)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(TimeUnit.SECONDS.name()), true), EJB3SubsystemModel.IDLE_TIMEOUT_UNIT)
                .end();

        // timer transformers
        subsystemBuilder.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH)
                .addChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, REFRESH_INTERVAL, ALLOW_EXECUTION)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REFRESH_INTERVAL, ALLOW_EXECUTION)
                .end();
    }

    /*
     * Transformers for changes in model version 4.0.0
     */
    private static void registerTransformers_3_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        subsystemBuilder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("hornetq-ra"), true), EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME)
                .end();

        // remote transformers
        subsystemBuilder.addChildResource(EJB3SubsystemModel.REMOTE_SERVICE_PATH)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CLIENT_MAPPINGS_CLUSTER_NAME)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_MAPPINGS_CLUSTER_NAME)
                .setDiscard(DiscardAttributeChecker.ALWAYS, EXECUTE_IN_WORKER) //as this does not affect functionality we just discard
                .end();

        // mdb delivery group transformers
        subsystemBuilder.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP));

        // strict max pool transformers
        subsystemBuilder.addChildResource(PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(StrictMaxPoolResourceDefinition.DeriveSize.NONE.toString())), DERIVE_SIZE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, DERIVE_SIZE)
                .end();
    }

    /*
     * Transformers for changes in model version 5.0.0
     */
    private static void registerTransformers_4_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        // application security domain
        subsystemBuilder.rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN));

        // identity transformers
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.IDENTITY_PATH);

        subsystemBuilder.addChildResource(RemotingProfileResourceDefinition.INSTANCE)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, StaticEJBDiscoveryDefinition.INSTANCE)
                .end();

        subsystemBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX)
                .end();

        subsystemBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN)
                .end();

        // Rename new statistics-enabled attribute to old enable-statistics
        subsystemBuilder.getAttributeBuilder()
                .addRename(EJB3SubsystemModel.STATISTICS_ENABLED, EJB3SubsystemModel.ENABLE_STATISTICS)
                .end();

        // added - debug (the transformed model is missing a value for log-server-exceptions
        subsystemBuilder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(ModelNode.TRUE, true), EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS)
                .end();
    }

    /*
     * Transformers for changes in model version 6.0.0
     */
    private static void registerTransformers_5_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        // interceptors
        subsystemBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS, EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS, EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS)
                .end();

        // thread pool
        subsystemBuilder.addChildResource(PathElement.pathElement(EJB3SubsystemModel.THREAD_POOL))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, PoolAttributeDefinitions.CORE_THREADS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, PoolAttributeDefinitions.CORE_THREADS)
                .end();
    }


    /*
     * Transformers for changes in model version 7.0.0
     */
    private static void registerTransformers_6_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {

        // default stateful session timeout
        subsystemBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT)
                .end();

    }

    /*
     * Transformers for changes in model version 8.0.0
     */
    @SuppressWarnings("deprecation")
    private static void registerTransformers_7_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        // Replaced <remote connector-ref="<connector>"/> with <remote connectors="<list of connectors>"/>
        // Both cannot be present. If connectors list > 1, reject; if connectors == 1, convert.
        subsystemBuilder.addChildResource(EJB3SubsystemModel.REMOTE_SERVICE_PATH).getAttributeBuilder()
                // to translate connectors to connector-ref
                .setDiscard(DiscardSingletonListAttributeChecker.INSTANCE, EJB3RemoteResourceDefinition.CONNECTORS)
                .addRejectCheck(RejectNonSingletonListAttributeChecker.INSTANCE, EJB3RemoteResourceDefinition.CONNECTORS)
                .setValueConverter(new SingletonListAttributeConverter(EJB3RemoteResourceDefinition.CONNECTORS), EJB3RemoteResourceDefinition.CONNECTOR_REF)
                .end();

        // Reject ejb3/remoting-profile=xxx/remote-http-connection
        subsystemBuilder.addChildResource(EJB3SubsystemModel.REMOTING_PROFILE_PATH)
                .rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.REMOTE_HTTP_CONNECTION));
    }

    /*
     * Transformers for changes in model version 9.0.0
     */
    private static void registerTransformers_8_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        // Replaced <remote connector-ref="<connector>"/> with <remote connectors="<list of connectors>"/>
        // Both cannot be present. If connectors list > 1, reject; if connectors == 1, convert.
        subsystemBuilder.addChildResource(EJB3SubsystemModel.REMOTE_SERVICE_PATH).getAttributeBuilder()
                // to translate connectors to connector-ref
                .setDiscard(DiscardSingletonListAttributeChecker.INSTANCE, EJB3RemoteResourceDefinition.CONNECTORS)
                .addRejectCheck(RejectNonSingletonListAttributeChecker.INSTANCE, EJB3RemoteResourceDefinition.CONNECTORS)
                .setValueConverter(new SingletonListAttributeConverter(EJB3RemoteResourceDefinition.CONNECTORS), EJB3RemoteResourceDefinition.CONNECTOR_REF)
                .end();

        // Reject ejb3/remoting-profile=xxx/remote-http-connection
        subsystemBuilder.addChildResource(EJB3SubsystemModel.REMOTING_PROFILE_PATH)
                .rejectChildResource(PathElement.pathElement(EJB3SubsystemModel.REMOTE_HTTP_CONNECTION));
        // Reject attribute legacy-compliant-principal-propagation
        subsystemBuilder.addChildResource(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, EJB3SubsystemModel.LEGACY_COMPLIANT_PRINCIPAL_PROPAGATION)
                .addRejectCheck(RejectAttributeChecker.DEFINED, EJB3SubsystemModel.LEGACY_COMPLIANT_PRINCIPAL_PROPAGATION)
                .end();
    }
    /*
     * This transformer is used with the datastores in /subsystem=ejb3/service=timer
     * <timer-service thread-pool-name= default-data-store=>
     *   <data-stores>
     *     <file-data-store name= path= relative-to=/>
     *     <database-data-store name= datasource-jndi-name= database= .../>
     *   </data-stores>
     * </timer-service>
     */
    private static class DataStoreTransformer implements CombinedTransformer {

        private DataStoreTransformer() {
        }

        /**
         * Registered against /subsystem=ejb3/service=timer/file-data-store=*:add(path=.., relative-to=..) only
         */
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

        /**
         * Registered against /subsystem=ejb3/service=timer only
         */
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
