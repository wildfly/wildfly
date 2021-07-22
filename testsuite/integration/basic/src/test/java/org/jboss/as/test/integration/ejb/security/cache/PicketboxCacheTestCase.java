/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ejb.security.cache;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.ejb.security.cache.RemotePicketboxCacheValidator.Result;
import org.jboss.as.test.integration.ejb.security.cache.RemotePicketboxCacheValidator.Status;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Test for checking the picketbox principal and credential are cacheable under
 * the same connection. This ensures that cache for security domains will work
 * as in previous releases.
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PicketboxCacheTestCase {

    private static final String DEPLOYMENT = PicketboxCacheTestCase.class.getSimpleName();
    private final String INVOCATION_URL = "remote+http://" +
            NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost")) + ":8080";

    @BeforeClass
    public static void beforeClass() {
        // Test for PicketBox cache - not supported in Elytron
        AssumeTestGroupUtil.assumeElytronProfileEnabled();
    }

    @Deployment(testable = false)
    public static Archive deployment() {
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage(PicketboxCacheValidatorBean.class.getPackage())
                .addAsResource(PermissionUtils.createPermissionsXmlAsset(
                        new RuntimePermission("org.jboss.security.getSecurityContext"),
                        new RuntimePermission("org.jboss.security.plugins.JBossSecurityContext.getSubjectInfo")),
                        "META-INF/jboss-permissions.xml");
    }

    @Test
    public void testCacheIsUsed() throws Exception {
        Properties prop = new Properties();
        prop.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        prop.put(Context.PROVIDER_URL, INVOCATION_URL);
        prop.put(Context.SECURITY_PRINCIPAL, "user1");
        prop.put(Context.SECURITY_CREDENTIALS, "password1");
        InitialContext ctx = null;
        try {
            ctx = new InitialContext(prop);
            String lookupName = "ejb:/" + DEPLOYMENT + "/" + PicketboxCacheValidatorBean.class.getSimpleName() + "!" + RemotePicketboxCacheValidator.class.getName();
            // first time
            RemotePicketboxCacheValidator ejb = (RemotePicketboxCacheValidator) ctx.lookup(lookupName);
            Result result = ejb.check();
            Assert.assertEquals("The logged user is not new to the cache", Status.NEW, result.getStatus());
            Assert.assertEquals("Username is not OK", "user1", result.getName());
            // second time to ensure the cache is used and the credential is the same
            ejb = (RemotePicketboxCacheValidator) ctx.lookup(lookupName);
            result = ejb.check();
            Assert.assertEquals("The logged user is not cached", Status.CACHED, result.getStatus());
            Assert.assertEquals("Username is not OK", "user1", result.getName());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
