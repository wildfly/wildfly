/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.subsystem;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.jsr77.subsystem.Constants.APP_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.DISTINCT_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.EJB_NAME;
import static org.jboss.as.jsr77.subsystem.Constants.MODULE_NAME;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.jsr77.ejb.ManagementEjbDeploymentInformation;
import org.jboss.as.jsr77.ejb.ManagementHomeEjbComponentView;
import org.jboss.as.jsr77.ejb.ManagementRemoteEjbComponentView;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.discovery.Discovery;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RegisterManagementEJBService implements Service<Void>{

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(ServiceName.of(JSR77ManagementExtension.SUBSYSTEM_NAME, "ejb"));

    final InjectedValue<DeploymentRepository> deploymentRepositoryValue = new InjectedValue<DeploymentRepository>();
    final InjectedValue<EJBClientContextService> ejbClientContextValue = new InjectedValue<>();
    final InjectedValue<MBeanServer> mbeanServerValue = new InjectedValue<MBeanServer>();
    final InjectedValue<AssociationService> associationServiceInjector = new InjectedValue<>();
    private volatile DeploymentModuleIdentifier moduleIdentifier;

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final DeploymentRepository repository = deploymentRepositoryValue.getValue();

        moduleIdentifier = new DeploymentModuleIdentifier(APP_NAME, MODULE_NAME, DISTINCT_NAME);

        final InjectedValue<ComponentView> injectedHomeView = new InjectedValue<ComponentView>();
        injectedHomeView.setValue(new ImmediateValue<ComponentView>(new ManagementHomeEjbComponentView()));

        final InjectedValue<ComponentView> injectedRemoteView = new InjectedValue<ComponentView>();
        injectedRemoteView.setValue(new ImmediateValue<ComponentView>(new ManagementRemoteEjbComponentView(mbeanServerValue.getValue())));

        Map<String, InjectedValue<ComponentView>> views = new HashMap<String, InjectedValue<ComponentView>>();
        views.put(ManagementHome.class.getName(), injectedHomeView);
        views.put(Management.class.getName(), injectedRemoteView);

        final EjbDeploymentInformation ejb = new ManagementEjbDeploymentInformation(EJB_NAME, views, SecurityActions.getClassLoader(this.getClass()));
        final ModuleDeployment deployment = new ModuleDeployment(moduleIdentifier, Collections.singletonMap(EJB_NAME, ejb));
        repository.add(moduleIdentifier, deployment);
        repository.startDeployment(moduleIdentifier);

        doPrivileged((PrivilegedAction<Void>) () -> {
            final ClassLoader classLoader = getClass().getClassLoader();
            EJBClientContext.getContextManager().setClassLoaderDefault(classLoader, ejbClientContextValue.getValue().getClientContext());
            Discovery.getContextManager().setClassLoaderDefault(classLoader, Discovery.create(associationServiceInjector.getValue().getLocalDiscoveryProvider()));
            return null;
        });
    }

    @Override
    public void stop(StopContext context) {
        deploymentRepositoryValue.getValue().remove(moduleIdentifier);
        doPrivileged((PrivilegedAction<Void>) () -> {
            final ClassLoader classLoader = getClass().getClassLoader();
            EJBClientContext.getContextManager().setClassLoaderDefault(classLoader, null);
            Discovery.getContextManager().setClassLoaderDefault(classLoader, null);
            return null;
        });
    }

}
