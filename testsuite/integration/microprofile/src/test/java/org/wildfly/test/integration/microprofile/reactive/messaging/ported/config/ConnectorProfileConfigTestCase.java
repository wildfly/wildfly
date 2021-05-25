/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.config;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.await;

import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
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
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
public class ConnectorProfileConfigTestCase {

    @Deployment
    public static WebArchive createArchive() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-connector-profile-cfg.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(DumbConnector.class, BeanUsingDummyConnector.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ConnectorProfileConfigTestCase.class.getPackage(),
                        "dummy-connector-with-profile.properties", "classes/META-INF/microprofile-config.properties")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }

    @Inject
    BeanUsingDummyConnector bean;

    @Test
    public void testThatTestProfileValuesAreUsed() {
        await(() -> bean.getList().size() == 2);
        ReactiveMessagingTestUtils.checkList(bean.getList(), "ola", "OLA");
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
}
