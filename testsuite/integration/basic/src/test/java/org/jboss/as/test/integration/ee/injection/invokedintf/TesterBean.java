/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.invokedintf;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

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
