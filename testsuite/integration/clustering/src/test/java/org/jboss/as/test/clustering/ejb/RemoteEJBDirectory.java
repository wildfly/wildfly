/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.ejb;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * {@link EJBDirectory} that uses remote JNDI.
 *
 * NOTE:
 * if you hold a static reference to this class, it causes any JNDI lookups across different tests to use the same discovered node registry (DNR).
 * This can cause server starts and stops in one test to contaminate other tests and produce incorrect results.
 * It is advisable to use one instance per test by defining a @Before and @After method to create and dispose of the instance on a per test basis.
 *
 * @author Paul Ferraro
 */
public class RemoteEJBDirectory extends NamingEJBDirectory {

    private static Properties createEnvironment() {
        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        // TODO UserTransaction lookup currently requires environment to be configured with provider URLs.
        // env.setProperty(Context.PROVIDER_URL, String.join(",", EJBClientContext.getCurrent().getConfiguredConnections().stream().map(EJBClientConnection::getDestination).map(URI::toString).collect(Collectors.toList())));
        return env;
    }

    public RemoteEJBDirectory(String module) throws NamingException {
        this(module, createEnvironment());
    }

    public RemoteEJBDirectory(String module, Properties properties) throws NamingException {
        super(properties, "ejb:", module, "txn:UserTransaction");
    }

    @Override
    protected String createJndiName(String beanName, Class<?> beanInterface, Type type) {
        String jndiName = super.createJndiName(beanName, beanInterface, type);
        switch (type) {
            case STATEFUL: {
                return jndiName + "?stateful=true";
            }
            default: {
                return jndiName;
            }
        }
    }
}
