/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.as.test.integration.ee.injection.invokedintf;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateless
public class TesterBean implements Tester {
    private static final Logger log = Logger.getLogger(TesterBean.class);

    @EJB
    private Remote1 annotated1;

    @EJB
    private Remote2 annotated2;

    private Remote1 xml1;

    private Remote2 xml2;

    private void check(String name, CommonRemote bean, Class<?> expectedInvokedBusinessInterface) throws TestFailedException {
        if (bean == null)
            throw new TestFailedException(name + " was not injected");
        Class<?> invokedBusinessInterface = bean.getInvokedBusinessInterface();
        log.trace("invokedBusinessInterface = " + invokedBusinessInterface);
        if (!invokedBusinessInterface.equals(expectedInvokedBusinessInterface))
            throw new TestFailedException("InvokedBusinessInterface was " + invokedBusinessInterface + " instead of "
                    + expectedInvokedBusinessInterface);
    }

    public void testAnnotated1() throws TestFailedException {
        check("annotated1", annotated1, Remote1.class);
    }

    public void testAnnotated2() throws TestFailedException {
        check("annotated2", annotated2, Remote2.class);
    }

    public void testXml1() throws TestFailedException {
        check("xml1", xml1, Remote1.class);
    }

    public void testXml2() throws TestFailedException {
        check("xml2", xml2, Remote2.class);
    }
}
