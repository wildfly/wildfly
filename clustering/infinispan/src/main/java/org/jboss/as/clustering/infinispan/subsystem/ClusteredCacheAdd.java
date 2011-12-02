package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.manager.CacheContainer;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Base class for clustered cache add operations
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class ClusteredCacheAdd extends CacheAdd {

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

    abstract Configuration processModelNode(ModelNode model, Configuration overrides, List<AdditionalDependency> additionalDeps) ;

    void performClusteredCacheRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

       // create a Configuration holding the operation data and final config
        Configuration overrides = new Configuration() ;

       // create a list for dependencies which may need to be added during processing
        List<AdditionalDependency> additionalDeps = new LinkedList<AdditionalDependency>() ;

        // pass in the model, not the operation
        processModelNode(model, overrides, additionalDeps) ;

        // get container and cache addresses
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR)) ;
        PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1) ;

        // get container and cache names
        String cacheName = cacheAddress.getLastElement().getValue() ;
        String containerName = containerAddress.getLastElement().getValue() ;

        // get container and cache service names
        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName) ;
        ServiceName cacheServiceName = containerServiceName.append(cacheName) ;

        // get container Model
        Resource rootResource = context.getRootResource() ;
        ModelNode container = rootResource.navigate(containerAddress).getModel() ;

        // get default cache of the container
        String defaultCache = container.require(ModelKeys.DEFAULT_CACHE).asString() ;

        // get start mode of the cache
        StartMode startMode = operation.hasDefined(ModelKeys.START) ? StartMode.valueOf(operation.get(ModelKeys.START).asString()) : StartMode.LAZY;

        // get the JNDI name of the container and its binding info
        String jndiName = CacheContainerAdd.getContainerJNDIName(container, containerName);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName) ;

        // setup configuration helper
        CacheService.CacheConfigurationHelperImpl helper = new CacheService.CacheConfigurationHelperImpl(cacheName) ;

        // install the cache service
        ServiceTarget target = context.getServiceTarget() ;
        ServiceName serviceName = EmbeddedCacheManagerService.getServiceName(containerName).append(cacheName) ;
        CacheService<Object, Object> cacheService = new CacheService<Object, Object>(cacheName, overrides, helper);

        ServiceBuilder<Cache<Object,Object>> builder = target.addService(cacheServiceName, cacheService) ;
        builder.addDependency(containerServiceName, CacheContainer.class, helper.getCacheContainerInjector()) ;
        builder.addDependency(EmbeddedCacheManagerDefaultsService.SERVICE_NAME, EmbeddedCacheManagerDefaults.class, helper.getDefaultsInjector());
        builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, helper.getTransactionManagerInjector());
        builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, helper.getTransactionSynchronizationRegistryInjector());
        // builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, helper.getXAResourceRecoveryRegistryInjector())

        builder.addDependency(bindInfo.getBinderServiceName()) ;

        // add in a REQUIRED dependency on ChannelService (containerName) to fail startup if JGroups subsystem not present
        builder.addDependency(ChannelService.getServiceName(containerName)) ;

        // add in any additional dependencies
        for (AdditionalDependency dep : additionalDeps) {
            builder.addDependency(dep.getName(), dep.getType(), dep.getTarget()) ;
        }

        builder.setInitialMode(startMode.getMode());
        // add an alias for the default cache
        if (cacheName.equals(defaultCache)) {
            builder.addAliases(CacheService.getServiceName(containerName,  null));
        }
        // blah
        if (startMode.getMode() == ServiceController.Mode.ACTIVE) {
            builder.addListener(verificationHandler);
        }

        // if we are clustered, update TransportRequiredService via its reference
        setTransportRequired(context, containerServiceName);

        newControllers.add(builder.install());
        log.debugf("cache %s installed for container %s", cacheName, containerName);
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
            log.debugf("Service TransportRequired not installed for container %s ", container.toString());
        }

        // set the value of the AtomicBoolean to value
        ((AtomicBoolean) serviceController.getValue()).set(true) ;
   }


}
