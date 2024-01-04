/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import java.util.Properties;
import java.util.concurrent.Callable;
import javax.naming.Context;
import javax.naming.InitialContext;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

/**
 * Test that {@link InitialContext} properties (principal+credentials) takes priority over the Elytron authentication
 * configuration.
 *
 * @author Josef Cacek
 */
public class InitialContextPropertiesOverrideTestCase extends AbstractSecurityContextPropagationTestBase {

    /**
     * Test that {@link InitialContext} properties (principal+credentials) takes priority over the Elytron authentication
     * configuration.
     *
     * <pre>
     * When: EJB client calls WhoAmIBean using both Elytron AuthenticationContext API InitialContext properties to set
     *       username/password combination
     * Then: username/password combination from InitialContext is used
     * </pre>
     */
    @Test
    public void testInitialContextPropertiesOverride() throws Exception {
        // Let's call the WhoAmIBean with different username+password combinations in Elytron API and InitialContext properties
        Callable<String> callable = () -> {
            final Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            jndiProperties.put(Context.PROVIDER_URL, server2.getApplicationRemotingUrl());
            jndiProperties.put(Context.SECURITY_PRINCIPAL, "whoami");
            jndiProperties.put(Context.SECURITY_CREDENTIALS, "whoami");
            final Context context = new InitialContext(jndiProperties);

            final WhoAmI bean = (WhoAmI) context.lookup(
                    SeccontextUtil.getRemoteEjbName(WAR_WHOAMI, "WhoAmIBean", WhoAmI.class.getName(), isWhoAmIStateful()));
            return bean.getCallerPrincipal().getName();
        };
        // Elytron API uses "entry" user, the InitialContext uses "whoami"
        String whoAmI = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AC_AUTHENTICATION);
        // The identity should be created from InitialContext properties
        assertEquals("The whoAmIBean.whoAmI() returned unexpected principal", "whoami", whoAmI);
    }

    @Override
    protected boolean isEntryStateful() {
        return false;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return false;
    }
}
