/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.Naming;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-859] On same named private lifecycle callbacks the super class callback is not called
 *
 * https://issues.jboss.org/browse/AS7-859
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
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
        assertTrue("Child @PostConstruct has not been called", Child.postConstructCalled);
        assertTrue("Parent @PostConstruct has not been called", Parent.postConstructCalled);
    }

}
