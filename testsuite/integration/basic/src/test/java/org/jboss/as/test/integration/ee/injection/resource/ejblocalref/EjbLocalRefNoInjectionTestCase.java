/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
