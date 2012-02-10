package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheRemove extends AbstractRemoveStepHandler {

    private static final Logger log = Logger.getLogger(CacheRemove.class.getPackage().getName());
    static final CacheRemove INSTANCE = new CacheRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        // get container and cache addresses
        final PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR)) ;
        final PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1) ;
        // get container and cache names
        final String cacheName = cacheAddress.getLastElement().getValue() ;
        final String containerName = containerAddress.getLastElement().getValue() ;

        // remove all services started by CacheAdd, in reverse order
        // remove the binder service
        String jndiName = (model.hasDefined(ModelKeys.JNDI_NAME) ?
                InfinispanJndiName.toJndiName(model.get(ModelKeys.JNDI_NAME).asString()) :
                InfinispanJndiName.defaultCacheJndiName(containerName, cacheName)).getAbsoluteName();
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        context.removeService(bindInfo.getBinderServiceName()) ;
        // remove the CacheService instance
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName).append(cacheName));
        // remove the cache configuration service
        context.removeService(CacheConfigurationService.getServiceName(containerName, cacheName));

        log.debugf("cache %s removed for container %s", cacheName, containerName);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
