package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Base class for clustered cache add operations
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredCacheAdd extends CacheAdd {

    private static Logger log = Logger.getLogger(ClusteredCacheAdd.class.getPackage().getName());

    /**
     * Transfer operation ModelNode values to model ModelNode values
     *
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    void populateClusteredCacheModelNode(ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String cacheType = cacheAddress.getLastElement().getKey();

        populateCacheModelNode(operation, model);

        // figure out the basic cache mode to use based on the cache type in the address
        Configuration.CacheMode cacheMode = null ;
        if (cacheType.equals(ModelKeys.INVALIDATION_CACHE))
            cacheMode = Configuration.CacheMode.INVALIDATION_SYNC;
        else if (cacheType.equals(ModelKeys.REPLICATED_CACHE))
            cacheMode = Configuration.CacheMode.REPL_SYNC ;
        else if (cacheType.equals(ModelKeys.DISTRIBUTED_CACHE))
            cacheMode = Configuration.CacheMode.DIST_SYNC ;
        else {
            ModelNode exception = new ModelNode() ;
            exception.get(FAILURE_DESCRIPTION).set("Cache type not recognized.") ;
            throw new OperationFailedException(exception);
        }
        model.get(ModelKeys.CACHE_MODE).set(cacheMode.name()) ;

        // now modify that cache mode based on any MODE attribute passed in
        if (operation.hasDefined(ModelKeys.MODE)) {
            Mode syncMode  = Mode.valueOf(operation.get(ModelKeys.MODE).asString());
            model.get(ModelKeys.CACHE_MODE).set(syncMode.apply(cacheMode).name()) ;
        }
        // System.out.println("cache mode = " + model.get(ModelKeys.CACHE_MODE).asString());

        populate(operation, model);
    }

    /**
     * Transfer elements common to both operations and models
     *
     * @param operation
     * @param model
     */
    protected static void populate(ModelNode operation, ModelNode model) {

        if (operation.hasDefined(ModelKeys.MODE)) {
            model.get(ModelKeys.MODE).set(operation.get(ModelKeys.MODE)) ;
        }
        if (operation.hasDefined(ModelKeys.QUEUE_SIZE)) {
            model.get(ModelKeys.QUEUE_SIZE).set(operation.get(ModelKeys.QUEUE_SIZE)) ;
        }
        if (operation.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
            model.get(ModelKeys.QUEUE_FLUSH_INTERVAL).set(operation.get(ModelKeys.QUEUE_FLUSH_INTERVAL)) ;
        }
        if (operation.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
            model.get(ModelKeys.REMOTE_TIMEOUT).set(operation.get(ModelKeys.REMOTE_TIMEOUT)) ;
        }
    }

    /**
     * Create a Configuration object initialized from the data in the operation.
     *
     * @param model data representing cache configuration
     * @param configuration Configuration to add the data to
     * @return initialised Configuration object
     */
    Configuration processClusteredCacheModelNode(ModelNode model, Configuration configuration, List<AdditionalDependency> additionalDeps) {

        // process cache attributes and elements
        processCacheModelNode(model, configuration, additionalDeps);

        // process clustered cache attributes and elements
        FluentConfiguration fluent = configuration.fluent();
        if (model.hasDefined(ModelKeys.QUEUE_SIZE)) {
            fluent.async().replQueueMaxElements(model.get(ModelKeys.QUEUE_SIZE).asInt());
        }
        if (model.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
            fluent.async().replQueueInterval(model.get(ModelKeys.QUEUE_FLUSH_INTERVAL).asLong());
        }
        // TODO  - need to check cache mode before setting
        if (model.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
            // fluent.sync().replTimeout(cache.get(ModelKeys.REMOTE_TIMEOUT).asLong());
        }

        return configuration ;
    }

    protected void setTransportRequired(OperationContext context, ServiceName container) {
        ServiceName name = TransportRequiredService.getServiceName(container);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<AtomicBoolean> serviceController = null ;

        /*
        List<ServiceName> serviceNames = registry.getServiceNames() ;
        System.out.println("Service Names:");
        for (ServiceName serviceName : serviceNames) {
            System.out.println(serviceName);
        }
        */

        try {
            serviceController = (ServiceController<AtomicBoolean>) registry.getRequiredService(name) ;
        }
        catch(ServiceNotFoundException e) {
            log.debug("Service TransportRequired not installed for container " + container);
        }

        // set the value of the AtomicBoolean to value
        ((AtomicBoolean) serviceController.getValue()).set(true) ;
   }


}
