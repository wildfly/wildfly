/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.service;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * <p> This {@link org.jboss.msc.service.Service} starts the {@link org.picketlink.idm.PartitionManager} using the configuration loaded from the domain model and publishes it in JNDI.
 * </p>
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Pedro Igor
 */
public class PartitionManagerService implements Service<PartitionManager> {

    private static final String SERVICE_NAME_PREFIX = "PartitionManagerService";
    private final String jndiName;
    private final String alias;
    private final IdentityConfigurationBuilder configurationBuilder;
    private volatile PartitionManager partitionManager;

    public PartitionManagerService(String alias, String jndiName, IdentityConfigurationBuilder configurationBuilder) {
        this.alias = alias;
        this.jndiName = jndiName;
        this.configurationBuilder = configurationBuilder;
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(IDMExtension.SUBSYSTEM_NAME, SERVICE_NAME_PREFIX, alias);
    }

    public static ServiceName createIdentityStoreServiceName(String name, String configurationName, String storeType) {
        return ServiceName.JBOSS.append(IDMExtension.SUBSYSTEM_NAME, name, configurationName, storeType);
    }

    @Override
    public PartitionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.partitionManager;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting PartitionManagerService for [%s]", this.alias);
        this.partitionManager = new DefaultPartitionManager(this.configurationBuilder.buildAll());
        publishPartitionManager(context);
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping PartitionManagerService for [%s]", this.alias);
    }
    private void publishPartitionManager(StartContext context) {
        BindInfo bindInfo = createPartitionManagerBindInfo();
        ServiceName serviceName = bindInfo.getBinderServiceName();
        final BinderService binderService = new BinderService(serviceName.getCanonicalName());
        final ServiceBuilder<ManagedReferenceFactory> builder = context.getChildTarget()
                                                                .addService(serviceName, binderService)
                                                                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(this.jndiName));

        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(createServiceName(this.alias), PartitionManager.class, new Injector<PartitionManager>() {
            @Override
            public void inject(final PartitionManager value) throws InjectionException {
                binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                binderService.getManagedObjectInjector().uninject();
            }
        });

        builder.setInitialMode(Mode.PASSIVE).install();

        ROOT_LOGGER.boundToJndi("PartitionManager " + this.alias, bindInfo.getAbsoluteJndiName());
    }

    private BindInfo createPartitionManagerBindInfo() {
        return ContextNames.bindInfoFor(this.jndiName);
    }

    public String getName() {
        return this.alias;
    }
}
