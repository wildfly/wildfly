/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for wildcard (*) in ejb-name element of jboss-ejb3.xml
 * @author Jan Martiska
 */
@ExtendWith(ArquillianExtension.class)
public class EJBNameWildcardTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "ejb-name-wildcard-test.jar")
                .addPackage(BeanOne.class.getPackage())
                .addPackage(BeanTwo.class.getPackage())
                .addAsManifestResource(BeanOne.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    /*
     * Try to invoke a method which requires special privileges,
     * as defined in jboss-ejb3.xml using ejb-name=*,method-name=restrictedMethod.
     * It shouldn't be allowed.
     */
    @Test
    public void testWildcardRestrictedMethodOnBeanOne() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanOne.class).wildcardRestrictedMethod());
    }

    @Test
    public void testWildcardRestrictedMethodOnBeanTwo() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanTwo.class).wildcardRestrictedMethod());
    }

    /*
     * Try to invoke a method which is excluded
     * by jboss-ejb3.xml using ejb-name=*,method-name=excludedMethod
     * and shouldn't be callable at all.
     */
    @Test
    public void testWildcardExcludedMethodOnBeanOne() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanOne.class).wildcardExcludedMethod());
    }

    @Test
    public void testWildcardExcludedMethodOnBeanTwo() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanTwo.class).wildcardExcludedMethod());
    }

    /*
     * Try to invoke a method which is excluded
     * by jboss-ejb3.xml using ejb-name=*,method-name=excludedMethod
     * and shouldn't be callable at all.
     */
    @Test
    public void testLocalRestrictedMethodOnBeanTwo() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanTwo.class).localRestrictedMethod());
    }

    @Test
    public void testLocalExcludedMethodOnBeanTwo() throws Exception {
        assertThrows(EJBAccessException.class, () ->
            getRestrictedBean(BeanTwo.class).localExcludedMethod());
    }

    /*
     * Try to invoke not excluded / restricted methods
     */

    @Test
    public void testUnRestrictedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).unRestrictedMethod();
    }

    @Test
    public void testNotExcludedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).notExcludedMethod();
    }

    private <T> T getRestrictedBean(Class<T> clazz) throws NamingException {
        return (T) new InitialContext().lookup("java:global/ejb-name-wildcard-test/" + clazz.getSimpleName());
    }

}
