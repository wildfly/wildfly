/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbLocalRefNoInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(EjbLocalRefNoInjectionTestCase.class, EjbLocalRefInjectionServlet.class, NamedSLSB.class, SimpleSLSB.class, Hello.class);
        war.addAsWebInfResource(EjbLocalRefNoInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testNoInjectionPoint() throws Exception {
        Hello bean = (Hello) new InitialContext().lookup("java:comp/env/noInjection");
        assertEquals("Simple Hello", bean.sayHello());
    }
}
