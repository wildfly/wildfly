/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.cdi;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class CdiInjectionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, CdiInjectionTestCase.class.getSimpleName() + ".war")
                .add(new StringAsset("<beans bean-discovery-mode=\"annotated\"></beans>"), "WEB-INF/beans.xml")
                .addClasses(CDIApplication.class, CDIProvider.class, CDIResource.class, CDIBean.class);
    }

    @Inject
    private Instance<Application> applicationInstance;

    @Inject
    private Instance<CDIProvider> providerInstance;

    @Inject
    private Instance<CDIResource> resourceInstance;

    @Test
    public void checkApplication() {
        Assert.assertNotNull(applicationInstance);
        final Bean<Application> bean = applicationInstance.getHandle().getBean();
        checkBean(bean, ApplicationScoped.class, CDIApplication.class);
    }

    @Test
    public void checkProvider() {
        Assert.assertNotNull(providerInstance);
        final Bean<CDIProvider> bean = providerInstance.getHandle().getBean();
        checkBean(bean, ApplicationScoped.class, CDIProvider.class);
    }

    @Test
    public void checkResource() {
        Assert.assertNotNull(resourceInstance);
        final Bean<CDIResource> bean = resourceInstance.getHandle().getBean();
        checkBean(bean, RequestScoped.class, CDIResource.class);
        final Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        Assert.assertFalse("Expected the CDIBean to be injected into the CDIResource", injectionPoints.isEmpty());
        final InjectionPoint ip = injectionPoints.iterator().next();
        final Annotated annotated = ip.getAnnotated();
        Assert.assertEquals(String.format("Expected the injection point of the CDIResource.bean to be %s", CDIBean.class), annotated.getBaseType(), CDIBean.class);
    }

    private void checkBean(final Bean<?> bean, final Class<? extends Annotation> expectedScope, final Class<?> exepctedBeanClass) {
        Assert.assertNotNull(String.format("Expected a bean of type %s, but was null.", exepctedBeanClass.getName()), bean);
        final Class<?> foundScope = bean.getScope();
        Assert.assertEquals(String.format("Expected a scope of @%s for bean %s, but found @%s", expectedScope.getSimpleName(), expectedScope.getName(), foundScope.getSimpleName()), expectedScope, foundScope);
    }
}
