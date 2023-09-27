/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.contextduel;

import jakarta.ejb.EJBException;
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
 * Test JPA 2.0 section 7.6.3.1
 * "
 * If the component is a stateful session bean to which an extended persistence context has been
 * bound and there is a different persistence context bound to the JTA transaction,
 * an EJBException is thrown by the container.
 * "
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class AddEPC2TxAfterPCTestCase {
    private static final String ARCHIVE_NAME = "jpa_AddEPC2TxAfterPCTestCase";


    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(
                AddEPC2TxAfterPCTestCase.class,
                Employee.class,
                BMTEPCStatefulBean.class,
                CMTPCStatefulBean.class);
        jar.addAsManifestResource(AddEPC2TxAfterPCTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
        } catch (NamingException e) {
            throw e;
        }
    }

    @Test
    public void testShouldGetEjbExceptionBecauseEPCIsAddedToTxAfterPc() throws Exception {
        BMTEPCStatefulBean stateful = lookup("BMTEPCStatefulBean", BMTEPCStatefulBean.class);
        try {
            stateful.shouldThrowError();
        } catch (EJBException expected) {
            // success
        }
    }

    @Test
    public void testShouldNotGetError() throws Exception {
        BMTEPCStatefulBean stateful = lookup("BMTEPCStatefulBean", BMTEPCStatefulBean.class);
        stateful.shouldNotThrowError();
    }

}
