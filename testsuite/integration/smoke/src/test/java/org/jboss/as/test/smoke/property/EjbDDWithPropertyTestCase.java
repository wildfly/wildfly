/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.property;

import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author John Bailey
 */
@RunWith(Arquillian.class)
public class EjbDDWithPropertyTestCase {
    private static final String MODULE_NAME = "dd-based";

    private static final String JAR_NAME = MODULE_NAME + ".jar";

    @Deployment
    public static JavaArchive getDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(TestSessionBean.class.getPackage());
        jar.addAsManifestResource(TestSessionBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(TestSessionBean.class.getPackage(), "jboss.properties", "jboss.properties");
        return jar;
    }

    @Test
    public void testPropertyBasedEnvEntry() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = TestSessionBean.class.getSimpleName();
        TestBean bean = (TestBean) ctx.lookup("java:global/" + MODULE_NAME + "/" + ejbName + "!" + TestBean.class.getName());
        assertEquals("foo" + System.getProperty("file.separator") + "bar", bean.getValue());
    }

    @Test
    public void testPropertyBasedEnvEntryWithOverride() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = TestSessionBean.class.getSimpleName();
        TestBean bean = (TestBean) ctx.lookup("java:global/" + MODULE_NAME + "/" + ejbName + "!" + TestBean.class.getName());
        assertEquals("foo-|-bar", bean.getValueOverride());
    }
}
