/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.epcpropagation.slsbxpc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test should fail due to use of extended persistence context in a stateless bean.
 * Added for WFLY-69
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class FailBecauseOfXPCNotInSFSBTestCase {

    private static final String ARCHIVE_NAME = "jpa_FailBecauseOfXPCNotInSFSBTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(FailBecauseOfXPCNotInSFSBTestCase.class,
                StatelessBeanWithXPC.class
        );
        jar.addAsManifestResource(FailBecauseOfXPCNotInSFSBTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    @Test
    public void testSerialization() throws Exception {
        final String errorCode = "WFLYJPA0070";
        StatelessBeanWithXPC bean = lookup("StatelessBeanWithXPC", StatelessBeanWithXPC.class);
        try {
            bean.test();
        } catch (EJBException expected) {
            //expected.printStackTrace();
            Throwable cause = expected.getCause();
            while (cause != null && !(cause.getMessage().contains(errorCode))) {
                cause = cause.getCause();
            }
            assertTrue("expected IllegalStateException was not thrown", cause instanceof IllegalStateException);
            Assert.assertThat("Wrong error message", cause.getMessage(), containsString(errorCode));
        } catch (Throwable unexpected) {
            fail("unexcepted exception " + unexpected.toString());
        }
    }
}
