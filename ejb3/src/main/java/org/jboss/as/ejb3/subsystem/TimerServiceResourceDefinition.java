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
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.operations.global.UndefineAttributeHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandler;
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
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
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
                    .setAllowNull(true) //for backward compatibility!
                    .setDefaultValue(new ModelNode("default-file-store")) //for backward compatibility!
                    .build();
    @Deprecated
    static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(FileDataStoreResourceDefinition.PATH)
            .setDeprecated(ModelVersion.create(2,0))
            .build();
    @Deprecated
    static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(FileDataStoreResourceDefinition.RELATIVE_TO)
            .setDeprecated(ModelVersion.create(2,0))
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
        resourceRegistration.registerReadWriteAttribute(PATH,FileStoreForwarder.INSTANCE,FileStoreForwarder.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO,FileStoreForwarder.INSTANCE,FileStoreForwarder.INSTANCE);
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FileDataStoreResourceDefinition(pathManager));

        resourceRegistration.registerSubModel(DatabaseDataStoreResourceDefinition.INSTANCE);
    }

    static final class FileStoreForwarder implements OperationStepHandler{
        static FileStoreForwarder INSTANCE = new FileStoreForwarder();
        private FileStoreForwarder(){

        }
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            String op = operation.get(ModelDescriptionConstants.OP).asString();
            ModelNode defaultDataStore = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get(DEFAULT_DATA_STORE.getName());
            if (!defaultDataStore.isDefined()){
                defaultDataStore = DEFAULT_DATA_STORE.getDefaultValue();
            }
            PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
            address = address.append(EJB3SubsystemModel.FILE_DATA_STORE,defaultDataStore.asString());
            operation.get(ModelDescriptionConstants.ADDRESS).set(address.toModelNode());
            if (op.equals(ModelDescriptionConstants.ADD)){
                context.addStep(new ModelNode(), operation, FileDataStoreAdd.INSTANCE, OperationContext.Stage.MODEL, true);
            }else if (op.equals(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION)){
                context.addStep(new ModelNode(), operation, ReadAttributeHandler.INSTANCE, OperationContext.Stage.MODEL, true);
            }else if (op.equals(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION)){
                context.addStep(new ModelNode(), operation, WriteAttributeHandler.INSTANCE, OperationContext.Stage.MODEL, true);
            }else if (op.equals(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION)){
                context.addStep(new ModelNode(), operation, UndefineAttributeHandler.INSTANCE, OperationContext.Stage.MODEL, true);
            }
            context.stepCompleted();
        }
    }

    static void registerTransformers_1_1_0(ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder timerService = parent.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH);
        timerService.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, EJB3SubsystemModel.DEFAULT_DATA_STORE)//this is ok, as default-data-store only has any sense with new model, but it is always set!
                .end();
        timerService.discardOperations(ModelDescriptionConstants.ADD);
        timerService.setCustomResourceTransformer(DataStoreTransformer.INSTANCE);
        timerService.rejectChildResource(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
        timerService.addChildRedirection(EJB3SubsystemModel.FILE_DATA_STORE_PATH, new PathAddressTransformer() {
            @Override
            public PathAddress transform(PathElement current, Builder builder) {
                return builder.getCurrent();
            }
        })
                .addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(DataStoreTransformer.INSTANCE)
                .end();

    }

    private static class DataStoreTransformer implements CombinedTransformer {
        static final DataStoreTransformer INSTANCE = new DataStoreTransformer();

        private DataStoreTransformer() {

        }

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            Resource original = context.readResource(address);
            if (original.getChildren(EJB3SubsystemModel.FILE_DATA_STORE).size()>1){
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

           /* ModelNode transformedOp = new ModelNode();
            transformedOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
            ModelNode op1 = transformedOp.get(ModelDescriptionConstants.STEPS).add();
            op1.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            op1.get(ModelDescriptionConstants.ADDRESS).set(address.toModelNode());
            op1.get(ModelDescriptionConstants.NAME).set(EJB3SubsystemModel.PATH);
            op1.get(ModelDescriptionConstants.VALUE).set(operation.get(EJB3SubsystemModel.PATH));

            ModelNode op2 = transformedOp.get(ModelDescriptionConstants.STEPS).add();
            op2.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            op2.get(ModelDescriptionConstants.ADDRESS).set(address.toModelNode());
            op2.get(ModelDescriptionConstants.NAME).set(EJB3SubsystemModel.RELATIVE_TO);
            op2.get(ModelDescriptionConstants.VALUE).set(operation.get(EJB3SubsystemModel.RELATIVE_TO));*/
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
                throw new OperationFailedException("Only file-store configuration is supported on target");
            }
            if (transformed.hasDefined(EJB3SubsystemModel.PATH)) { //happens when first file store was already transformed
                throw new OperationFailedException(context.getLogger().getRejectedResourceWarning(address, null));
            } else {
                transformed.get(EJB3SubsystemModel.PATH).set(fileStore.get(EJB3SubsystemModel.PATH));
                transformed.get(EJB3SubsystemModel.RELATIVE_TO).set(fileStore.get(EJB3SubsystemModel.RELATIVE_TO));
            }
            context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            //do not process children!
        }
    }

}
