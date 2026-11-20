/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.noncomponent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that @Resource annotations on classes that are not managed by the container are ignored.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class NonComponentResourceInjectionTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "non-component.war");
        war.addPackage(NonComponentResourceInjectionTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testBindingDoesNotExist() throws NamingException {
        try {
            InitialContext context = new InitialContext();
            Object result = context.lookup("java:module/env/" + NonComponentResourceInjectionTestCase.class.getName() + "/userTransaction");
            Assertions.fail();
        } catch (NamingException expected) {

        }
    }


    @Test
    public void testBindingExists() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/" + ComponentResourceInjection.class.getName() + "/userTransaction");
        Assertions.assertNotNull(result);
    }

}
