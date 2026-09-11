/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;

import javax.naming.Context;
import java.util.Properties;

/**
 * Validates failover behavior of a remotely accessed @Stateful EJB behind a load balancer.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LoadBalancedRemoteStatefulEJBFailoverTestCase extends AbstractLoadBalancedRemoteStatefulEJBFailoverTestCase {
    private static final String MODULE_NAME = LoadBalancedRemoteStatefulEJBFailoverTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment(MODULE_NAME);
    }

    public LoadBalancedRemoteStatefulEJBFailoverTestCase() {
        super(() -> new RemoteEJBDirectory(MODULE_NAME, getInitialContextProperties()));
    }

    /**
     * Provide a configured Properties file which will be used to initialise the JNDI InitialContext used for
     * EJB proxy lookup. This specific configuration does two things:
     * - it causes the EJB client code to use the EJB/HTTP transport, as the scheme is http and the context path
     * is /wildfly-services
     * - it causes the EJB client to bypass discovery, and use only the target nodes identified in the PROVIDER_URL
     * in the case of retry of a failed invocation.
     *
     * @return an InitialContext properties file configured for EJB/HTTP
     */
    private static Properties getInitialContextProperties() {
        Properties properties = new Properties();
        // the load balancer is started at a process sharing the same IP with an offset port
        String lbAddress = TestSuiteEnvironment.getHttpAddress();
        int lbPort = TestSuiteEnvironment.getHttpPort() + LB_OFFSET;
        String providerURL = String.format("http://%s:%s/wildfly-services", lbAddress, lbPort);
        log.info("Setting PROVIDER_URL in InitialContext: " + providerURL);

        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        properties.put(Context.PROVIDER_URL, providerURL);
        properties.put(Context.SECURITY_PRINCIPAL, "remoteejbuser");
        properties.put(Context.SECURITY_CREDENTIALS, "rem@teejbpasswd1");
        return properties;
    }
}
