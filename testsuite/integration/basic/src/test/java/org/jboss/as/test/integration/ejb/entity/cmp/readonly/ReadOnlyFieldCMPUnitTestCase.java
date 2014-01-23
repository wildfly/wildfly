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
package org.jboss.as.test.integration.ejb.entity.cmp.readonly;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import static org.junit.Assert.fail;

/**
 * Basic cmp2 tests
 *
 * @author alex@jboss.org
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81036 $
 */
@RunWith(CmpTestRunner.class)
public class ReadOnlyFieldCMPUnitTestCase extends AbstractCmpTest {

    private static Logger log = Logger.getLogger(ReadOnlyFieldCMPUnitTestCase.class);

    private static final String ARCHIVE_NAME = "cmp-read-only-field.jar";
    private static final String ENTITY_LOOKUP = "java:module/SimpleEntity!" + SimpleEntityLocalHome.class.getName();

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addPackage(ReadOnlyFieldCMPUnitTestCase.class.getPackage());
        jar.addAsManifestResource(ReadOnlyFieldCMPUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(ReadOnlyFieldCMPUnitTestCase.class.getPackage(), "jbosscmp-jdbc_field_readonly.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private SimpleEntityLocalHome getSimpleHome() {
        try {
            return (SimpleEntityLocalHome) iniCtx.lookup(ENTITY_LOOKUP);
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getSimpleHome: " + e.getMessage());
        }
        return null;
    }

    private SimpleEntityLocal bart;
    private SimpleEntityLocal homer;
    private SimpleEntityLocal maggie;

    @Override
    public void setUpEjb() throws Exception {
        SimpleEntityLocalHome simpleHome = getSimpleHome();
        bart = simpleHome.findByPrimaryKey(1L);
        homer = simpleHome.findByPrimaryKey(2L);
    }

    @Test
    public void testUpdateHomer() throws FinderException {
        homer.setName("Marge");
    }

    @Test
    public void testCreateMaggie() throws CreateException {
        maggie = getSimpleHome().create(3L, "Maggie");
    }

    @Test(expected = EJBException.class)
    public void testUpdateReadOnlyBart() {
        try {
            bart.setLastName("TheKlown");
            fail("Shouldn't be able to update read-only CMP");
        } catch (EJBException ex) {
            assertThat(ex.getLocalizedMessage(), containsString("JBAS018521"));
        }
    }

    @Override
    public void tearDownEjb() throws Exception {
        if (maggie != null) {
            maggie.remove();
        }
    }
}
