/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.multipleinjections;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1118
 * <p>
 * Tests that injecting multiple persistence units into the same EJB works as expected
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class MultiplePersistenceContextInjectionsTestCase {

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multiplepuinjections.war");
        war.addPackage(MultiplePersistenceContextInjectionsTestCase.class.getPackage());
        // WEB-INF/classes is implied
        war.addAsResource(MultiplePersistenceContextInjectionsTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        return war;
    }


    @Test
    public void testMultiplePersistenceUnitInjectionsIntoSameBean() throws NamingException {
        MultipleInjectionsSfsb bean = (MultipleInjectionsSfsb) iniCtx.lookup("java:module/" + MultipleInjectionsSfsb.class.getSimpleName());
        Assert.assertNotNull(bean.getEntityManager());
        Assert.assertNotNull(bean.getEntityManager2());
        Assert.assertNotNull(bean.getExtendedEntityManager());
        Assert.assertNotNull(bean.getExtendedEntityManager2());
    }

}
