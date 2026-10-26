/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.common.Naming;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * [AS7-859] On same named private lifecycle callbacks the super class callback is not called
 *
 * https://issues.jboss.org/browse/AS7-859
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@ExtendWith(ArquillianExtension.class)
public class AS859TestCase {

    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, "as859.war")
                .addPackage(Child.class.getPackage())
                .addPackage(Naming.class.getPackage());
        return deployment;
    }

    @Test
    public void testPostConstruct() throws NamingException {
        final Child bean = Naming.lookup("java:global/as859/Child", Child.class);
        assertNotNull(bean);
        assertTrue(Child.postConstructCalled, "Child @PostConstruct has not been called");
        assertTrue(Parent.postConstructCalled, "Parent @PostConstruct has not been called");
    }

}
