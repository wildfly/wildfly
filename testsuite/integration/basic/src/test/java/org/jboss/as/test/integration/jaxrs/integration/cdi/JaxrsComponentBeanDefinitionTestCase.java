/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.integration.cdi;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class JaxrsComponentBeanDefinitionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.add(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "WEB-INF/beans.xml");
        war.addClasses(JaxrsComponentBeanDefinitionTestCase.class, CDIBean.class, CDIResource.class, CDIApplication.class, CDIProvider.class);
        return war;
    }

    @Inject
    BeanManager beanManager;

    @Test
    public void testResource() throws Exception {
        // There's one bean of type CDIResource and it's scope is @RequestScoped (this is resteasy-cdi specific)
        Set<Bean<?>> beans = beanManager.getBeans(CDIResource.class);
        assertEquals(1, beans.size());
        assertEquals(RequestScoped.class, beans.iterator().next().getScope());
    }

    @Test
    public void testApplication() throws Exception {
        // There's one bean of type CDIApplication and it's scope is @ApplicationScoped
        Set<Bean<?>> beans = beanManager.getBeans(CDIApplication.class);
        assertEquals(1, beans.size());
        assertEquals(ApplicationScoped.class, beans.iterator().next().getScope());
    }

    @Test
    public void testProvider() throws Exception {
        // There's one bean of type CDIProvider and it's scope is @ApplicationScoped
        Set<Bean<?>> beans = beanManager.getBeans(CDIProvider.class);
        assertEquals(1, beans.size());
        assertEquals(ApplicationScoped.class, beans.iterator().next().getScope());
    }

}
