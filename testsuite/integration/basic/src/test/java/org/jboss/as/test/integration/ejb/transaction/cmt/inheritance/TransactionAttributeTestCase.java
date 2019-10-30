/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.jboss.as.test.integration.ejb.transaction.cmt.inheritance;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Test checking behavior of {@link TransactionAttribute} when inherited
 * by child classes.
 * <p>
 * <b>8.3.7.1</b> Specification of Transaction Attributes with Metadata Annotations<br>
 * If the bean class has superclasses, the following additional rules apply.
 * <ul>
 *   <li>A transaction attribute specified on a superclass S applies to the business methods defined by
 *    S. If a class-level transaction attribute is not specified on S, it is equivalent to specification of
 *    TransactionAttribute(REQUIRED) on S.</li>
 *   <li>A transaction attribute may be specified on a business method M defined by class S to override
 *    for method M the transaction attribute value explicitly or implicitly specified on the class S.</li>
 *   <li>If a method M of class S overrides a business method defined by a superclass of S, the transaction
 *    attribute of M is determined by the above rules as applied to class S.</li>
 *  </ul>
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
public class TransactionAttributeTestCase {

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @EJB
    private SuperSLSB superClass;

    @EJB
    private ChildSLSB childClass;

    @EJB
    private ChildWithClassAnnotationSLSB childWithClassAnnotation;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "transaction-attribute-inheritance.war");
        war.addPackage(TransactionAttributeTestCase.class.getPackage());
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    @Test
    public void superClassCheck() throws Exception {
        tm.begin();
        try {
            // active transaction
            superClass.aMethod();
            superClass.bMethod();
            superClass.cMethod();
        } finally {
            tm.rollback();
        }

        tm.begin();
        try {
            // active transaction
            superClass.neverMethod();
            Assert.fail("TransactionAttribute.NEVER expects failure when running"
                    + " within a transactional context of txn: '" + tm.getTransaction() + "'");
        } catch (EJBException ignoreAsExpected) {
        } finally {
            tm.rollback();
        }

        // no active transaction
        superClass.aMethod();
        superClass.bMethod();
        superClass.cMethod();
        superClass.neverMethod();
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#REQUIRED}
     */
    @Test
    public void defaultBeanAttributeOverridesParentClassDeclaration() throws Exception {
        tm.begin();
        Transaction testTxn = tm.getTransaction();
        try {
            // active transaction
            Transaction beanTxn = childClass.aMethod();
            Assert.assertNotNull("There has to be started a transaction in the bean", beanTxn);
            Assert.assertEquals("Child method default REQUIRED TransactionAttribute"
                + " has to override the settings of SUPPORTS from super class", testTxn, beanTxn);
        } finally {
            tm.rollback();
        }

        // no active transaction
        Transaction beanTxn = childClass.aMethod();
        Assert.assertNotNull("There has to be started transaction in bean as TransactionAttribute"
            + " should be REQUIRED", beanTxn);
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#SUPPORTS}
     */
    @Test
    public void inheritsWhenMethodCalledFromParent() throws Exception {
        tm.begin();
        try {
            // active transaction
            childClass.bMethod();
        } finally {
            tm.rollback();
        }

        // no active transaction
        Assert.assertNull("There can't be active transaction here", tm.getTransaction());
        childClass.bMethod();
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#MANDATORY}
     */
    @Test
    public void explicitMethodAttributeOverrides() throws Exception {
        tm.begin();
        try {
            // active transaction
            childClass.cMethod();
        } finally {
            tm.rollback();
        }

        try {
            // no active transaction
            Assert.assertNull("There can't be active transaction here", tm.getTransaction());
            childClass.cMethod();
            Assert.fail("Method called with no active transaction but MANDATORY requires it");
        } catch (EJBException ignoreAsExpected) {
        }
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#REQUIRED}
     */
    @Test
    public void defaultBeanAttributeOverridesParentMethodDeclaration() throws Exception {
        tm.begin();
        Transaction testTxn = tm.getTransaction();
        try {
            // active transaction
            Transaction beanTxn = childClass.neverMethod();
            Assert.assertNotNull("There has to be started a transaction in the bean", beanTxn);
            Assert.assertEquals("Child method default REQUIRED TransactionAttribute"
                + " has to override the settings of SUPPORTS from super class", testTxn, beanTxn);
        } finally {
            tm.rollback();
        }

        // no active transaction
        Assert.assertNull("There can't be active transaction here", tm.getTransaction());
        Transaction beanTxn = childClass.neverMethod();
        Assert.assertNotNull("There has to be started a transaction in the bean "
            + "as TransactionAttribute is REQUIRED", beanTxn);
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#NEVER}
     */
    @Test
    public void classAnnotationOverridesParentDeclaration() throws Exception {
        tm.begin();
        try {
            // active transaction
            childWithClassAnnotation.aMethod();
            Assert.fail("TransactionAttribute.NEVER expects failure when running"
                + " within a transactional context of txn: '" + tm.getTransaction() + "'");
        } catch (EJBException ignoreAsExpected) {
        } finally {
            tm.rollback();
        }

        // no active transaction
        Assert.assertNull("There can't be active transaction here", tm.getTransaction());
        childWithClassAnnotation.aMethod();
    }

    /**
     * Tests expects behavior of {@link TransactionAttributeType#SUPPORTS}
     */
    @Test
    public void inheritsWhenMethodCalledFromParentWithChildClassLevelAnnotation() throws Exception {
        tm.begin();
        try {
            // active transaction
            childClass.bMethod();
        } finally {
            tm.rollback();
        }

        // no active transaction
        Assert.assertNull("There can't be active transaction here", tm.getTransaction());
        childClass.bMethod();
    }
}
