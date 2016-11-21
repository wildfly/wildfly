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
package org.jboss.as.weld;

import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.validation.ValidatorFactory;

import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.ee.beanvalidation.LazyValidatorFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service that replaces the delegate of LazyValidatorFactory with a CDI-enabled
 * ValidatorFactory.
 *
 * @author Farah Juma
 */
public class CdiValidatorFactoryService implements Service<CdiValidatorFactoryService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("CdiValidatorFactoryService");

    private final InjectedValue<BeanManager> beanManagerInjector = new InjectedValue<>();

    private final ClassLoader classLoader;

    private final DeploymentUnit deploymentUnit;

    /**
     * Create the CdiValidatorFactoryService instance.
     *
     * @param deploymentUnit the deployment unit
     */
    public CdiValidatorFactoryService(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
        final Module module = this.deploymentUnit.getAttachment(Attachments.MODULE);
        this.classLoader = module.getClassLoader();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            BeanManager beanManager = beanManagerInjector.getValue();

            // Get the CDI-enabled ValidatorFactory
            ValidatorFactory validatorFactory = getReference(ValidatorFactory.class, beanManager);

            // Replace the delegate of LazyValidatorFactory
            LazyValidatorFactory lazyValidatorFactory = (LazyValidatorFactory)(deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY));
            lazyValidatorFactory.replaceDelegate(validatorFactory);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    @Override
    public void stop(final StopContext context) {
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            ValidatorFactory validatorFactory = deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
            if (validatorFactory != null) {
                validatorFactory.close();
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    @Override
    public CdiValidatorFactoryService getValue()
            throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private <T> T getReference(Class<T> clazz, BeanManager beanManager) {
        Set<Bean<?>> beans = beanManager.getBeans(clazz, new AnnotationLiteral<Default>() {});
        Iterator<Bean<?>> i = beans.iterator();
        if (!i.hasNext()) {
            return null;
        }

        Bean<?> bean = i.next();
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, clazz, context);
    }

    public Injector<BeanManager> getBeanManagerInjector() {
        return beanManagerInjector;
    }

}
