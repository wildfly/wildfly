/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.multinode.clientinterceptor.secured;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.ejb.EJBAccessException;
import javax.security.auth.AuthPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.as.test.shared.integration.interceptor.clientside.AbstractClientInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.interceptor.clientside.InterceptorModule;
import org.jboss.as.test.shared.util.ClientInterceptorUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * A test case verifying client-side interceptor execution while accessing an EJB with @RolesAllowed, @DenyAll and @PermitAll security annotations.
 * See https://issues.jboss.org/browse/WFLY-6144 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({SecuredBeanClientInterceptorTestCase.SetupTask.class, EjbSecurityDomainSetup.class})
public class SecuredBeanClientInterceptorTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "secured-bean-test-client";
    private static final String ARCHIVE_NAME_SERVER = "secured-bean-test-server";

    private static final String moduleName = "secured-bean-interceptor-module";

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SERVER + ".jar");
        jar.addClasses(Secured.class, SecuredBean.class);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        final Package currentPackage = SecuredBeanClientInterceptorTestCase.class.getPackage();

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME_CLIENT + ".war");
        war.addClasses(Util.class, ClientInterceptorUtil.class);
        war.addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class);
        war.addClasses(SecuredBeanClientInterceptorTestCase.class, SampleSecureInterceptor.class, Secured.class, SecuredBean.class);
        war.addPackage(AbstractClientInterceptorsSetupTask.class.getPackage());
        war.addAsResource(currentPackage, "users.properties", "users.properties");
        war.addAsResource(currentPackage, "roles.properties", "roles.properties");
        war.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        war.addAsWebInfResource(currentPackage, "jboss-web.xml", "jboss-web.xml");
        war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF");
        war.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron"),
                        new ElytronPermission("getSecurityDomain"),
                        new RuntimePermission("getProtectionDomain"),
                        new AuthPermission("modifyPrincipals")),
                "permissions.xml");
        return war;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("client")
    public void securedBeanAccessCheck() throws Exception {
        Secured bean = ClientInterceptorUtil.lookupStatelessRemote(ARCHIVE_NAME_SERVER, SecuredBean.class, Secured.class);
        Assert.assertNotNull(bean);

        String result = bean.permitAll("permitAll");
        Assert.assertNotNull(result);

        try {
            bean.denyAll("denyAll");
            Assert.fail("it was supposed to throw EJBAccessException");
        }
        catch (EJBAccessException ie){
            // expected
        }
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("client")
    public void securedBeanRoleCheck() throws Exception {
        final Callable<Void> callable = () -> {
            Secured bean = ClientInterceptorUtil.lookupStatelessRemote(ARCHIVE_NAME_SERVER, SecuredBean.class, Secured.class);

            try {
                bean.roleEcho("role1access");
            } catch (EJBAccessException ignored) {
            }

            try {
                bean.role2Echo("access");
            } catch (EJBAccessException e) {
                // expected
            }
            Assert.assertEquals(0, SampleSecureInterceptor.latch.getCount());
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    static class SetupTask extends AbstractClientInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    SampleSecureInterceptor.class,
                    moduleName,
                    "module.xml",
                    SecuredBeanClientInterceptorTestCase.class.getResource("module.xml"),
                    "client-side-interceptor-secured.jar"
            ));
        }
    }
}
