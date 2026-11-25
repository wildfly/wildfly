/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests using an EAR with a single application client module and not specifying the name
 * of the appclient artifact from the command line.
 *
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UnspecifiedApplicationClientTestCase  extends AbstractSimpleApplicationClientTestCase {

    private static final String APP_NAME = UnspecifiedApplicationClientTestCase.class.getSimpleName();
    private static EnterpriseArchive archive;

    public UnspecifiedApplicationClientTestCase() {
        super(APP_NAME);
    }

    @Override
    public Archive<?> getArchive() {
        return UnspecifiedApplicationClientTestCase.archive;
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        archive = buildAppclientEar(APP_NAME, true);
        return archive;
    }

    @Test
    public void simpleAppClientTest() throws Exception {
        super.testAppClient(null, null, "${test.expr.applcient.param:cmdLineParam}", "cmdLineParam");
    }
}
