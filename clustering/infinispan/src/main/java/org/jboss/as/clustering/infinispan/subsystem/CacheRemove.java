package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheRemove extends AbstractRemoveStepHandler {

    static final CacheRemove INSTANCE = new CacheRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String cacheType = getCacheType(operation) ;

        if (cacheType.equals(ModelKeys.LOCAL_CACHE)) {
            LocalCacheAdd.INSTANCE.removeRuntimeServices(context, operation, model);
        } else if (cacheType.equals(ModelKeys.INVALIDATION_CACHE)) {
            InvalidationCacheAdd.INSTANCE.removeRuntimeServices(context, operation, model);
        } else if (cacheType.equals(ModelKeys.REPLICATED_CACHE)) {
            ReplicatedCacheAdd.INSTANCE.removeRuntimeServices(context, operation, model);
        } else if (cacheType.equals(ModelKeys.DISTRIBUTED_CACHE)) {
            DistributedCacheAdd.INSTANCE.removeRuntimeServices(context, operation, model);
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode cacheModel) throws OperationFailedException {

        // we also need the containerModel to re-install cache services
        final PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1) ;
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        // re-add the services if the remove failed
        String cacheType = getCacheType(operation) ;
        ServiceVerificationHandler verificationHandler = null ;

        if (cacheType.equals(ModelKeys.LOCAL_CACHE)) {
            LocalCacheAdd.INSTANCE.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
        } else if (cacheType.equals(ModelKeys.INVALIDATION_CACHE)) {
            InvalidationCacheAdd.INSTANCE.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
        } else if (cacheType.equals(ModelKeys.REPLICATED_CACHE)) {
            ReplicatedCacheAdd.INSTANCE.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
        } else if (cacheType.equals(ModelKeys.DISTRIBUTED_CACHE)) {
            DistributedCacheAdd.INSTANCE.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
        }
    }

    private String getCacheType(ModelNode operation) {

        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
//        String cacheName = cacheAddress.getLastElement().getValue();
        String cacheType = cacheAddress.getLastElement().getKey();

        return cacheType ;
    }
}
