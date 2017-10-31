/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
