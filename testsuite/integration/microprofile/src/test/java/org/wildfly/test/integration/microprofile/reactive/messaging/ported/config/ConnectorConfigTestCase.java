/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.config;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.await;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.checkList;

import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils;

/**
 * Copied from Quarkus and adjusted
 */
@ServerSetup({EnableReactiveExtensionsSetupTask.class, ConnectorConfigTestCase.SetConfigPropertiesSetupTask.class})
@RunWith(Arquillian.class)
public class ConnectorConfigTestCase {
    @Deployment
    public static WebArchive enableExtensions() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-connector-cfg.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(DumbConnector.class, BeanUsingDummyConnector.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class,
                        EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class, SetConfigPropertiesSetupTask.class)
                // Rather than using Quarkus's overrideConfigKey() method which we don't have, we set the config
                // properties via the SetConfigPropertiesSetupTask
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }

    @Inject
    BeanUsingDummyConnector bean;

    @Test
    public void test() {
        await(() -> bean.getList().size() == 2);
        checkList(bean.getList(), "bonjour", "BONJOUR");
    }

    @ApplicationScoped
    public static class BeanUsingDummyConnector {

        private List<String> list = new CopyOnWriteArrayList<>();

        @Incoming("a")
        public void consume(String s) {
            list.add(s);
        }

        public List<String> getList() {
            return list;
        }

    }

    public static class SetConfigPropertiesSetupTask extends CLIServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            NodeBuilder nb = this.builder.node(containerId);
            nb.setup("/system-property=mp.messaging.incoming.a.values:add(value=bonjour)");
            nb.setup("/system-property=mp.messaging.incoming.a.connector:add(value=dummy)");
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            NodeBuilder nb = this.builder.node(containerId);
            nb.teardown("/system-property=mp.messaging.incoming.a.values:remove");
            nb.teardown("/system-property=mp.messaging.incoming.a.connector:remove");
            super.tearDown(managementClient, containerId);
        }
    }
}
