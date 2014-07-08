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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the timer-service resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TimerServiceResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DATA_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DATA_STORE, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowNull(false)
                    //.setDefaultValue(new ModelNode("default-file-store")) //for backward compatibility!
                    .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES ;

    private final PathManager pathManager;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(THREAD_POOL_NAME.getName(), THREAD_POOL_NAME);
        map.put(DEFAULT_DATA_STORE.getName(), DEFAULT_DATA_STORE);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    public TimerServiceResourceDefinition(final PathManager pathManager) {
        super(EJB3SubsystemModel.TIMER_SERVICE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE),
                TimerServiceAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.pathManager = pathManager;
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FileDataStoreResourceDefinition(pathManager));

        resourceRegistration.registerSubModel(DatabaseDataStoreResourceDefinition.INSTANCE);
    }

    static void registerTransformers_1_1_0(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder timerService = parent.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        registerDataStoreTransformers(timerService, true);
    }

    static void registerTransformers_1_2_0(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder timerService = parent.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        registerDataStoreTransformers(timerService, false);
    }

    private static void registerDataStoreTransformers(ResourceTransformationDescriptionBuilder timerService, boolean rejectPathExpressions) {

        DataStoreTransformer dataStoreTransformer = new DataStoreTransformer(rejectPathExpressions);
        timerService.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, EJB3SubsystemModel.DEFAULT_DATA_STORE)//this is ok, as default-data-store only has any sense with new model, but it is always set!
                .end();
        timerService.discardOperations(ModelDescriptionConstants.ADD);
        timerService.setCustomResourceTransformer(dataStoreTransformer);
        timerService.rejectChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
        ResourceTransformationDescriptionBuilder fileDataStore = timerService.addChildRedirection(EJB3SubsystemModel.FILE_DATA_STORE_PATH, new PathAddressTransformer() {
            @Override
            public PathAddress transform(PathElement current, Builder builder) {
                return builder.getCurrent();
            }
        });

        if (rejectPathExpressions) {
            fileDataStore = fileDataStore.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, FileDataStoreResourceDefinition.PATH)
                .end();
        }
        fileDataStore.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
            .inheritResourceAttributeDefinitions()
            .setCustomOperationTransformer(dataStoreTransformer)
            .end();

    }

    private static class DataStoreTransformer implements CombinedTransformer {

        private final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");

        private final boolean rejectPathExpression;

        private DataStoreTransformer(boolean rejectPathExpression) {
            this.rejectPathExpression = rejectPathExpression;
        }

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            Resource original = context.readResourceFromRoot(address);
            if (original.getChildren(EJB3SubsystemModel.FILE_DATA_STORE).size() > 1){
                return new TransformedOperation(operation,new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return context.getLogger().getRejectedResourceWarning(address,operation);
                    }
                }, OperationResultTransformer.ORIGINAL_RESULT);
            }
            operation.get(THREAD_POOL_NAME.getName()).set(original.getModel().get(THREAD_POOL_NAME.getName()));
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            Resource untransformedResource = context.readResource(PathAddress.EMPTY_ADDRESS);
            ModelNode untransformedModel = Resource.Tools.readModel(untransformedResource);
            String defaultDataStore = untransformedModel.get(DEFAULT_DATA_STORE.getName()).asString();

            ModelNode transformed = resource.getModel();
            transformed.remove(DEFAULT_DATA_STORE.getName());
            ModelNode fileStore = untransformedModel.get(EJB3SubsystemModel.FILE_DATA_STORE, defaultDataStore);
            if (!fileStore.isDefined()) {//happens where default is not file-store
                rejectIncompatibleDataStores(context, address);
                // If we get here the slave must be 7.1 and we don't know if the slave has this profile ignored
                // Just use an empty "file-store" which won't work on a slave server anyway because a null 'path'
                // won't work in FileTimerPersistence
                fileStore = new ModelNode();
            } else if ((untransformedModel.hasDefined(EJB3SubsystemModel.DATABASE_DATA_STORE)
                            && untransformedModel.get(EJB3SubsystemModel.DATABASE_DATA_STORE).keys().size() > 0)
                        || untransformedModel.get(EJB3SubsystemModel.FILE_DATA_STORE).keys().size() > 1) {
                rejectIncompatibleDataStores(context, address);
            }

            ModelNode path = fileStore.get(EJB3SubsystemModel.PATH);
            if (rejectPathExpression) {
                rejectPathExpression(context, address, defaultDataStore, path);
            }
            transformed.get(EJB3SubsystemModel.PATH).set(path);
            transformed.get(EJB3SubsystemModel.RELATIVE_TO).set(fileStore.get(EJB3SubsystemModel.RELATIVE_TO));

            context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            //do not process children!
        }

        private void rejectIncompatibleDataStores(ResourceTransformationContext context, PathAddress address) throws OperationFailedException {
            TransformationTarget tgt = context.getTarget();
            if (tgt.isIgnoredResourceListAvailableAtRegistration()) {
                // Slave is 7.2.x or higher and we know this resource is not ignored
                throw new OperationFailedException(EjbLogger.ROOT_LOGGER.untransformableTimerService(address));
            } else {
                // 7.1.x slave; resource *may* be ignored so we can't fail; just log
                context.getLogger().logWarning(EjbLogger.ROOT_LOGGER.untransformableTimerService(address));
            }
        }

        private void rejectPathExpression(ResourceTransformationContext context, PathAddress address, String dataStoreName, ModelNode pathAttribute) throws OperationFailedException {

            if (pathAttribute.getType() == ModelType.EXPRESSION
                    || (pathAttribute.getType() == ModelType.STRING && EXPRESSION_PATTERN.matcher(pathAttribute.asString()).matches())) {

                PathAddress fileStoreAddress =
                        PathAddress.pathAddress(address, PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, dataStoreName));

                TransformationTarget tgt = context.getTarget();
                if (tgt.isIgnoredResourceListAvailableAtRegistration()) {
                    // Slave is 7.2.x or higher and we know this resource is not ignored
                    List<String> msg = Collections.singletonList(context.getLogger().getAttributeWarning(fileStoreAddress, null, ControllerLogger.ROOT_LOGGER.attributesDontSupportExpressions(), FileDataStoreResourceDefinition.PATH.getName()));
                    throw ControllerLogger.ROOT_LOGGER.rejectAttributesCoreModelResourceTransformer(fileStoreAddress, tgt.getHostName(), tgt.getVersion(), msg);
                } else {
                    // 7.1.x slave; resource *may* be ignored so we can't fail; just log
                    context.getLogger().logAttributeWarning(fileStoreAddress, ControllerLogger.ROOT_LOGGER.attributesDontSupportExpressions(), FileDataStoreResourceDefinition.PATH.getName());
                }
            }
        }
    }

}
