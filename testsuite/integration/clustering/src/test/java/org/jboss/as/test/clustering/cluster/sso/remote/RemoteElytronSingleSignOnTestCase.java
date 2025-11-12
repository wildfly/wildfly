/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.sso.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.sso.AbstractSingleSignOnTestCase;
import org.jboss.as.test.clustering.cluster.sso.ElytronSSOServerSetupTask;
import org.jboss.as.test.clustering.cluster.sso.IdentityServerSetupTask;
import org.jboss.as.test.integration.web.sso.SSOTestBase;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup({ InfinispanServerSetupTask.class, RemoteElytronSingleSignOnTestCase.ServerSetupTask.class, ElytronSSOServerSetupTask.class, IdentityServerSetupTask.class })
public class RemoteElytronSingleSignOnTestCase extends AbstractSingleSignOnTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createArchive();
    }

    private static Archive<?> createArchive() {
        return SSOTestBase.createSsoEar();
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(NODE_1_2.toArray(new String[0]))
                    .setup("/subsystem=distributable-web/hotrod-single-sign-on-management=other:add(remote-cache-container=sso, cache-configuration=default)")
                    .teardown("/subsystem=distributable-web/hotrod-single-sign-on-management=other:remove")
            ;
        }
    }
}
