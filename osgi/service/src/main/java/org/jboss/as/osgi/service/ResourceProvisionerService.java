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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.provision.AbstractResourceProvisioner;
import org.jboss.osgi.provision.ProvisionException;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.repository.RepositoryContent;

/**
 * The standalone {@link Provisioner} service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-May-2012
 */
public final class ResourceProvisionerService extends AbstractService<XResourceProvisioner> {

    private final InjectedValue<XResolver> injectedResolver = new InjectedValue<XResolver>();
    private final InjectedValue<XPersistentRepository> injectedRepository = new InjectedValue<XPersistentRepository>();
    private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
    private XResourceProvisioner provisioner;

    public static ServiceController<?> addService(final ServiceTarget target) {
        ResourceProvisionerService service = new ResourceProvisionerService();
        ServiceBuilder<?> builder = target.addService(OSGiConstants.PROVISION_SERVICE_NAME, service);
        builder.addDependency(OSGiConstants.REPOSITORY_SERVICE_NAME, XPersistentRepository.class, service.injectedRepository);
        builder.addDependency(Services.RESOLVER, XResolver.class, service.injectedResolver);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, service.injectedBundleContext);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private ResourceProvisionerService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        final XResolver resolver = injectedResolver.getValue();
        final XPersistentRepository repository = injectedRepository.getValue();
        final BundleContext syscontext = injectedBundleContext.getValue();
        provisioner = new AbstractResourceProvisioner(resolver, repository) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> List<T> installResources(List<XResource> resources) throws ProvisionException {
                List<T> result = new ArrayList<T>();
                for (XResource res : resources) {
                    try {
                        String symbolicName = res.getIdentityCapability().getSymbolicName();
                        String location = "provisioned-resource/" + symbolicName;
                        InputStream input = ((RepositoryContent) res).getContent();
                        Bundle bundle = syscontext.installBundle(location, input);
                        result.add((T) bundle);
                    } catch (BundleException ex) {
                        throw new ProvisionException(ex);
                    }
                }
                return Collections.unmodifiableList(result);
            }
        };
    }

    @Override
    public synchronized void stop(StopContext context) {
        provisioner = null;
    }

    @Override
    public synchronized XResourceProvisioner getValue() throws IllegalStateException {
        return provisioner;
    }
}
