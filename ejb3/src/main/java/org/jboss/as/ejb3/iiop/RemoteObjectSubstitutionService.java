/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.iiop;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.ejb.client.AbstractEJBMetaData;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EntityEJBMetaData;
import org.jboss.javax.rmi.RemoteObjectSubstitution;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHandle;
import org.jboss.ejb.client.EJBHomeHandle;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBMetaDataImpl;
import org.jboss.ejb.iiop.EJBMetaDataImplIIOP;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.ejb.HomeHandle;

/**
 * @author Stuart Douglas
 */
public class RemoteObjectSubstitutionService implements RemoteObjectSubstitution, Service<RemoteObjectSubstitution> {

    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "iiop", "remoteObjectSubstitution");

    @Override
    public Object writeReplaceRemote(final Object object) {


        final DeploymentRepository deploymentRepository = deploymentRepositoryInjectedValue.getOptionalValue();
        //if we are not started yet just return
        if (deploymentRepository == null) {
            return object;
        }

        if (EJBClient.isEJBProxy(object)) {
            return createIIOPReferenceForBean(object, deploymentRepository);
        } else if (object instanceof EJBHandle) {
            final EJBHandle<?> handle = (EJBHandle<?>) object;
            final EJBLocator<?> locator = handle.getLocator();
            final EjbIIOPService factory = serviceForLocator(locator, deploymentRepository);
            if (factory != null) {
                return factory.handleForLocator(locator);
            }
        } else if (object instanceof EJBHomeHandle) {
            final EJBHomeHandle<?> handle = (EJBHomeHandle<?>) object;
            final EJBLocator<?> locator = handle.getLocator();
            final EjbIIOPService factory = serviceForLocator(locator, deploymentRepository);
            if (factory != null) {
                return factory.handleForLocator(locator);
            }
        } else if (object instanceof EJBMetaDataImpl) {
            final EJBMetaDataImpl metadata = (EJBMetaDataImpl) object;
            Class<?> pk = null;
            if (!metadata.isSession()) {
                pk = metadata.getPrimaryKeyClass();
            }
            final EJBLocator<?> locator = EJBClient.getLocatorFor(metadata.getEJBHome());
            final EjbIIOPService factory = serviceForLocator(locator, deploymentRepository);
            return new EJBMetaDataImplIIOP(metadata.getRemoteInterfaceClass(), metadata.getHomeInterfaceClass(), pk, metadata.isSession(), metadata.isStatelessSession(), (HomeHandle) factory.handleForLocator(locator));
        } else if (object instanceof AbstractEJBMetaData) {
            final AbstractEJBMetaData<?, ?> metadata = (AbstractEJBMetaData<?, ?>) object;
            final EJBHomeLocator<?> locator = metadata.getHomeLocator();
            final EjbIIOPService factory = serviceForLocator(locator, deploymentRepository);
            Class<?> pk = metadata instanceof EntityEJBMetaData ? metadata.getPrimaryKeyClass() : null;
            return new EJBMetaDataImplIIOP(metadata.getRemoteInterfaceClass(), metadata.getHomeInterfaceClass(), pk, metadata.isSession(), metadata.isStatelessSession(), (HomeHandle) factory.handleForLocator(locator));
        }
        return object;
    }

    private Object createIIOPReferenceForBean(Object object, DeploymentRepository deploymentRepository) {
        EJBLocator<? extends Object> locator;
        try {
            locator = EJBClient.getLocatorFor(object);
        } catch (Exception e) {
            //not an EJB proxy
            locator = null;
        }
        if (locator != null) {
            final EjbIIOPService factory = serviceForLocator(locator, deploymentRepository);
            if (factory != null) {
                return factory.referenceForLocator(locator);
            }
        }
        return object;
    }

    private EjbIIOPService serviceForLocator(final EJBLocator<?> locator, DeploymentRepository deploymentRepository) {
        final ModuleDeployment module = deploymentRepository.getModules().get(new DeploymentModuleIdentifier(locator.getAppName(), locator.getModuleName(), locator.getDistinctName()));
        if (module == null) {
            EjbLogger.ROOT_LOGGER.couldNotFindEjbForLocatorIIOP(locator);
            return null;
        }
        final EjbDeploymentInformation ejb = module.getEjbs().get(locator.getBeanName());
        if (ejb == null) {
            EjbLogger.ROOT_LOGGER.couldNotFindEjbForLocatorIIOP(locator);
            return null;
        }
        final EjbIIOPService factory = ejb.getIorFactory();
        if (factory == null) {
            EjbLogger.ROOT_LOGGER.ejbNotExposedOverIIOP(locator);
            return null;
        }
        return factory;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public RemoteObjectSubstitution getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjectedValue() {
        return deploymentRepositoryInjectedValue;
    }
}
