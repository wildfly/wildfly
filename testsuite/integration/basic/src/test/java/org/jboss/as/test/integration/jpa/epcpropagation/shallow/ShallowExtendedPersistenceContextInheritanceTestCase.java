/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.shallow;

import static org.junit.Assert.assertNotNull;

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
 * Test JPA 2.0 section 7.6.2.1
 * <p>
 * Inheritance of Extended Persistence Context
 * <p>
 * "
 * If a stateful session bean instantiates a stateful session bean (executing in the same EJB container instance)
 * which also has such an extended persistence context, the extended persistence context of the first stateful
 * session bean is inherited by the second stateful session bean and bound to it, and this rule recursively
 * applies—independently of whether transactions are active or not at the point of the creation of the stateful
 * session beans.
 * "
 * <p>
 * This tests the "shallow" inheritance where the above only applies to parent/child relationships (not recursively
 * up and vertically which is "deep" inheritance).
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ShallowExtendedPersistenceContextInheritanceTestCase {
    private static final String ARCHIVE_NAME = "jpa_ShallowExtendedPersistenceContextInheritanceTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(
                ShallowExtendedPersistenceContextInheritanceTestCase.class,
                SecondDAO.class,
                FirstDAO.class,
                TopLevelBean.class);

        jar.addAsManifestResource(ShallowExtendedPersistenceContextInheritanceTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        jar.addAsManifestResource(ShallowExtendedPersistenceContextInheritanceTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
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

    /**
     * With shallow extended persistence context inheritance, sibling beans do not inherit the XPC from each other.
     * If sibling beans are involved in the same transaction, an EJBException will be thrown.
     * This test ensures that the EJBException is thrown.
     *
     * @throws Exception
     */
    @Test
    public void testShouldGetEjbExceptionBecauseEPCIsAddedToTxAfterPc() throws Exception {
        TopLevelBean topLevelBean = lookup("TopLevelBean", TopLevelBean.class);

        Throwable error = null;
        // excepted error will be something like:
        //  jakarta.ejb.EJBException: WFLYJPA0030:
        //  Found extended persistence context in SFSB invocation call stack but that cannot be used
        //  because the transaction already has a transactional context associated with it...
        try {
            topLevelBean.referenceTwoDistinctExtendedPersistenceContextsInSameTX_fail();
        } catch (EJBException caught) {
            error = caught;
        }

        assertNotNull("with shallow inheritance, sibling beans do not inherit extended persistence " +
                "context from each other (unless ExtendedPersistenceInheritance.DEEP is specified)", error);

    }

    /**
     * With both DEEP and SHALLOW extended persistence context inheritance, a bean creating another local bean via
     * JNDI lookup will also use the extended persistence context inheritance rules.  This tests that the JNDI lookup
     * does use the same XPC.
     * <p>
     * See http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-06/message/28 for more details than
     * the JPA 2.0 specification includes.
     *
     * @throws Exception
     */
    @Test
    public void testChildInheritanceFromParentExtendedPersistenceContextViaBusinessMethodUsingJndiLookup() throws Exception {
        TopLevelBean topLevelBean = lookup("TopLevelBean", TopLevelBean.class);

        topLevelBean.induceCreationViaJNDILookup();
    }

    /**
     * With both DEEP and SHALLOW extended persistence context inheritance, a bean creating another local bean via
     * JNDI lookup will also use the extended persistence context inheritance rules.  This tests that two levels of JNDI
     * lookup does use the same XPC.
     * <p>
     * See http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-06/message/28 for more details than
     * the JPA 2.0 specification includes.
     *
     * @throws Exception
     */
    @Test
    public void testChildInheritanceFromParentExtendedPersistenceContextViaBusinessMethodUsingTwoLevelJndiLookup() throws Exception {
        TopLevelBean topLevelBean = lookup("TopLevelBean", TopLevelBean.class);

        topLevelBean.induceCreationViaJNDILookup();
    }


}
