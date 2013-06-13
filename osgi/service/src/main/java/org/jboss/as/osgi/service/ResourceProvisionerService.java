/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.service;

import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper.ServerDeploymentException;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.provision.AbstractResourceProvisioner;
import org.jboss.osgi.provision.ProvisionException;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResource;
import org.osgi.service.repository.RepositoryContent;

/**
 * The standalone {@link Provisioner} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-May-2012
 */
public final class ResourceProvisionerService extends AbstractService<XResourceProvisioner> {

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<XPersistentRepository> injectedRepository = new InjectedValue<XPersistentRepository>();
    private final InjectedValue<XResolver> injectedResolver = new InjectedValue<XResolver>();
    private ModelControllerClient modelControllerClient;
    private XResourceProvisioner provisioner;

    public static ServiceController<?> addService(final ServiceTarget target) {
        ResourceProvisionerService service = new ResourceProvisionerService();
        ServiceBuilder<?> builder = target.addService(OSGiConstants.PROVISIONER_SERVICE_NAME, service);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedController);
        builder.addDependency(RepositoryService.SERVICE_NAME, XPersistentRepository.class, service.injectedRepository);
        builder.addDependency(OSGiConstants.ABSTRACT_RESOLVER_SERVICE_NAME, XResolver.class, service.injectedResolver);
        return builder.install();
    }

    private ResourceProvisionerService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        ModelController modelController = injectedController.getValue();
        modelControllerClient = modelController.createClient(Executors.newCachedThreadPool());

        final XResolver resolver = injectedResolver.getValue();
        final XPersistentRepository repository = injectedRepository.getValue();
        final ServerDeploymentHelper serverDeployer = new ServerDeploymentHelper(modelControllerClient);
        provisioner = new AbstractResourceProvisioner(resolver, repository, XResource.TYPE_BUNDLE) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> List<T> installResources(List<XResource> resources, Class<T> type) throws ProvisionException {
                List<T> result = new ArrayList<T>();
                for (XResource res : resources) {
                    String name = res.getIdentityCapability().getName();
                    InputStream input = ((RepositoryContent) res).getContent();
                    try {
                        String runtimeName = serverDeployer.deploy(name, input);
                        result.add((T)runtimeName);
                    } catch (ServerDeploymentException ex) {
                       throw new ProvisionException(ex);
                    }
                }
                return Collections.unmodifiableList(result);
            }
        };
    }

    @Override
    public synchronized void stop(StopContext context) {
        try {
            modelControllerClient.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public synchronized XResourceProvisioner getValue() throws IllegalStateException {
        return provisioner;
    }
}
