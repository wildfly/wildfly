/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.as.test.integration.ejb.security.runasprincipal.customdomain.EntryBean;
import org.jboss.as.test.integration.ejb.security.runasprincipal.customdomain.TargetBean;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * RunAsPrincipal test across legacy security domains.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ RunAsPrincipalCustomDomainTestCase.PropertyFilesSetup.class,
        RunAsPrincipalCustomDomainTestCase.SecurityDomainsSetup.class })
@Category(CommonCriteria.class)
@RunAsClient
public class RunAsPrincipalCustomDomainTestCase {

    private static final String DEPLOYMENT = "runasprincipal-test";

    @Deployment(name = DEPLOYMENT, testable = false, order = 1)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar").addClasses(WhoAmI.class, EntryBean.class,
                TargetBean.class);
    }

    private WhoAmI lookupEntryBean() throws Exception {
        final Properties pr = new Properties();
        pr.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        return (WhoAmI) new InitialContext(pr)
                .lookup("ejb:/" + DEPLOYMENT + "/" + EntryBean.class.getSimpleName() + "!" + WhoAmI.class.getName());
    }

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeElytronProfileEnabled(); // PicketBox specific feature - not supported in Elytron
    }

    @Test
    public void test() throws Exception {
        Callable<String> callable = () -> {
            return lookupEntryBean().getCallerPrincipal();
        };
        String caller = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().useName("guest").usePassword("guest").useRealm("ApplicationRealm")
                                .useHost(Utils.getDefaultHost(false)).usePort(8080)
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5")))
                .runCallable(callable);
        assertEquals("Unexpected principal name returned", "principalFromEntryBean", caller);
    }

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final Map<String, String> lmOptions = new HashMap<String, String>();
            lmOptions.put("usersProperties", PropertyFilesSetup.FILE_USERS.getAbsolutePath());
            lmOptions.put("rolesProperties", PropertyFilesSetup.FILE_ROLES.getAbsolutePath());

            final SecurityDomain sd = new SecurityDomain.Builder().name(DEPLOYMENT)
                    .loginModules(new SecurityModule.Builder().name("UsersRoles").flag("required").options(lmOptions).build())
                    .build();

            return new SecurityDomain[] { sd };
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates property files with users and roles.
     */
    static class PropertyFilesSetup implements ServerSetupTask {

        public static final File FILE_USERS = new File("test-users.properties");
        public static final File FILE_ROLES = new File("test-roles.properties");

        /**
         * Generates property files.
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.writeStringToFile(FILE_USERS, "target=target", "ISO-8859-1");
            FileUtils.writeStringToFile(FILE_ROLES, "target=Target", "ISO-8859-1");
        }

        /**
         * Removes generated property files.
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            FILE_USERS.delete();
            FILE_ROLES.delete();
        }
    }

}
