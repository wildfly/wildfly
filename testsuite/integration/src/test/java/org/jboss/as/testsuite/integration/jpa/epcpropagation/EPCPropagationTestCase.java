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
package org.jboss.as.testsuite.integration.jpa.epcpropagation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For managed debugging:
 * -Djboss.options="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y"
 * <p/>
 * For embedded debugging:
 *  start AS standalone and attach debugger
 *  mvn install -Premote
 *
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class EPCPropagationTestCase {
    private static final String ARCHIVE_NAME = "epc-propagation";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(
            CMTEPCStatefulBean.class, CMTStatefulBean.class, EPCStatefulBean.class,
            InitEPCStatefulBean.class, IntermediateStatefulBean.class, IntermediateStatefulInterface.class,
            MyEntity.class, NoTxEPCStatefulBean.class, NoTxStatefulBean.class,
            StatefulBean.class, StatefulInterface.class, StatelessBean.class,
            StatelessInterface.class, AbstractStatefulInterface.class, EPCPropagationTestCase.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;
    
    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:module/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testBMTPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(1, "EntityName");

        StatefulInterface stateful = lookup("StatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(1, "EntityName");

        assertTrue("Name changes should propagate", equal);
    }

    @Test
    public void testBMTEPCPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(2, "EntityName");

        StatefulInterface stateful = lookup("EPCStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(2, "EntityName");

        assertTrue("Name changes should propagate", equal);
    }

    @Test
    public void testCMTPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(3, "EntityName");

        StatefulInterface stateful = lookup("CMTStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(3, "EntityName");

        assertTrue("Name changes should propagate", equal);
    }

    @Test
    public void testCMTEPCPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(4, "EntityName");

        StatefulInterface stateful = lookup("CMTEPCStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(4, "EntityName");

        assertTrue("Name changes should propagate", equal);
    }

    @Test
    public void testNoTxPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(5, "EntityName");

        StatefulInterface stateful = lookup("NoTxStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(5, "EntityName");

        assertFalse("Name changes should not propagate", equal);
    }

    /**
     * Ensure that AS7-1663 is fixed, extended persistence context should not be propagated if there is no JTA transaction.
     *
     * @throws Exception
     */
    @Test
    public void testNoTxEPCPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(6, "EntityName");

        StatefulInterface stateful = lookup("NoTxEPCStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(6, "EntityName");

        assertFalse("Name changes should not propagate (non-tx SFSB XPC shouldn't see SLSB PC changes)", equal);
    }

    @Test
    public void testIntermediateEPCPropagation() throws Exception {
        StatelessInterface stateless = lookup("StatelessBean", StatelessInterface.class);
        stateless.createEntity(7, "EntityName");

        StatefulInterface stateful = lookup("InitEPCStatefulBean", StatefulInterface.class);
        boolean equal = stateful.execute(7, "EntityName");

        assertTrue("Name changes should propagate", equal);
    }

    @Test
    public void testXPCPostConstruct() throws Exception {
        StatefulInterface stateful = lookup("EPCStatefulBean", StatefulInterface.class);
        assertNull("stateful postConstruct operation should success: " + stateful.getPostConstructErrorMessage(), stateful.getPostConstructErrorMessage());
    }

    /**
     * JPA 7.6.2 XPC is closed when dependent session bean(s) are closed/destroyed.
     *
     * During this test, an entity (X) will be created in the XPC but not persisted to the database.
     * When the last SFSB referencing the XPC is closed, the entity (X) will no longer be found.
     *
     * @throws Exception
     */
    @Test
    public void testNoTxEPCRemoveOperation() throws Exception {

        StatefulInterface stateful = lookup("NoTxEPCStatefulBean", StatefulInterface.class);
        boolean equal = stateful.createEntity(8, "EntityName Master X");

        assertTrue("XPC inheritance should copy entity to other SFSB created on SFSB invocation call", equal);

        // stateful should be the only SFSB left

        // create another sfsb on the invocation to stateful (should inherit the same XPC used above.
        StatefulInterface anotherStateful = stateful.createSFSBOnInvocation();
        stateful.finishUp();  // destroy SFSB
        stateful = null;      // clear reference to avoid using it by accident
                              // both entities (8,9) should still be in XPC
        equal = anotherStateful.createEntity(9,"John Steed");
        assertTrue("again, XPC inheritance should copy entity to other SFSB created on SFSB invocation call", equal);
    }


}
