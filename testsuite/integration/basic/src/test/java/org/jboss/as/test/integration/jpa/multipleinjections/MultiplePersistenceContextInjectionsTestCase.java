/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
