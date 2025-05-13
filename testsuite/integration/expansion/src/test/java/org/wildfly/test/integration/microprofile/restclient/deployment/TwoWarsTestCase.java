/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient.deployment;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests that two deployed WAR's can interact with each other.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TwoWarsTestCase extends AbstractMultipleDeploymentsTest {

    @Deployment(testable = false, name = DEPLOYMENT_1)
    public static WebArchive deployClient() throws IOException {
        return createWar(DEPLOYMENT_1, DEPLOYMENT_2);
    }

    @Deployment(testable = false, name = DEPLOYMENT_2)
    public static WebArchive deployServer() throws IOException {
        return createWar(DEPLOYMENT_2, DEPLOYMENT_1);
    }
}
