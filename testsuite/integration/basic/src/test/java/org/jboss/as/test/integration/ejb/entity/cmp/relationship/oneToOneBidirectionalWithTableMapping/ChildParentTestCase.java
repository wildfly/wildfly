/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping;

import javax.naming.InitialContext;
import static org.junit.Assert.fail;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class ChildParentTestCase extends AbstractCmpTest {

    static org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(ChildParentTestCase.class);
    
    @ArquillianResource
    protected InitialContext iniCtx;
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "one2one-table-mapping.jar");
        jar.addPackage(ChildParentTestCase.class.getPackage());
        jar.addAsManifestResource(ChildParentTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(ChildParentTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        jar.addAsManifestResource(ChildParentTestCase.class.getPackage(), "jboss.xml", "jboss.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private ChildLocalHome getChildHome() {
        try {
            Object objRef = iniCtx.lookup(ChildLocalHome.JNDI_NAME);
            // only narrow if necessary
            if (java.rmi.Remote.class.isAssignableFrom(ChildLocalHome.class)) {
                return (ChildLocalHome) javax.rmi.PortableRemoteObject.narrow(objRef, ChildLocalHome.class);
            } else {
                return (ChildLocalHome) objRef;
            }
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getChildHome: " + e.getMessage());
        }
        return null;
    }

    private ParentLocalHome getParentHome() {
        try {
            Object objRef = iniCtx.lookup(ParentLocalHome.JNDI_NAME);
            // only narrow if necessary
            if (java.rmi.Remote.class.isAssignableFrom(ParentLocalHome.class)) {
                return (ParentLocalHome) javax.rmi.PortableRemoteObject.narrow(objRef, ParentLocalHome.class);
            } else {
                return (ParentLocalHome) objRef;
            }
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getParentHome: " + e.getMessage());
        }
        return null;
    }

    @Test
    public void testCreateParentThenChild() throws Exception {
        ParentLocalHome parentHome = getParentHome();
        ParentLocal parent = parentHome.create(new ParentPK(1));
        ChildLocalHome childHome = getChildHome();
        ChildLocal child = childHome.create(new ChildPK(1));
    }

    public void tearDownEjb() throws Exception {
        ParentLocalHome parentHome = getParentHome();
        ParentLocal parent = parentHome.findByPrimaryKey(new ParentPK(1));
        parent.remove();
        ChildLocalHome childHome = getChildHome();
        ChildLocal child = childHome.findByPrimaryKey(new ChildPK(1));
        child.remove();
    }
}
