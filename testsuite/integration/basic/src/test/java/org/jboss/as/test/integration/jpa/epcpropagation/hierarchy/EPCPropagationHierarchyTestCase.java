/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1121 Tests that JPA interceptors are correctly registered if injection is done into the superclass of a SFSB
 * <p>
 * TODO: AS7-1120 Also tests that persistence context inheritance (JPA 7.6.2.1) works correctly when injecting one SFSB into another directly
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EPCPropagationHierarchyTestCase {
    private static final String ARCHIVE_NAME = "epc-hierarchy-propagation";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(EPCPropagationHierarchyTestCase.class.getPackage());
        jar.addAsManifestResource(EPCPropagationHierarchyTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testPersistenceContextPropagation() throws Exception {
        ChildStatefulBean stateful = lookup("ChildStatefulBean", ChildStatefulBean.class);
        stateful.testPropagation();
    }


}
