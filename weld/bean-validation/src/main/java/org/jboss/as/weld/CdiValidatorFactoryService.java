/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.validation.ValidatorFactory;

import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.ee.beanvalidation.LazyValidatorFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service that replaces the delegate of LazyValidatorFactory with a Jakarta Contexts and Dependency Injection enabled
 * ValidatorFactory.
 *
 * @author Farah Juma
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class CdiValidatorFactoryService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.of("CdiValidatorFactoryService");

    private final Supplier<BeanManager> beanManagerSupplier;

    private final ClassLoader classLoader;

    private final DeploymentUnit deploymentUnit;

    /**
     * Create the CdiValidatorFactoryService instance.
     *
     * @param deploymentUnit the deployment unit
     */
    public CdiValidatorFactoryService(final DeploymentUnit deploymentUnit, final Supplier<BeanManager> beanManagerSupplier) {
        this.deploymentUnit = deploymentUnit;
        final Module module = this.deploymentUnit.getAttachment(Attachments.MODULE);
        this.classLoader = module.getClassLoader();
        this.beanManagerSupplier = beanManagerSupplier;
    }

    @Override
    public void start(final StartContext context) {
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);

            // Get the Jakarta Contexts and Dependency Injection enabled ValidatorFactory
            ValidatorFactory validatorFactory = getReference(ValidatorFactory.class, beanManagerSupplier.get());

            // Replace the delegate of LazyValidatorFactory
            LazyValidatorFactory lazyValidatorFactory = (LazyValidatorFactory)(deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY));
            lazyValidatorFactory.replaceDelegate(validatorFactory);
            lazyValidatorFactory.replaceDelegate(validatorFactory.getConstraintValidatorFactory());
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

}
