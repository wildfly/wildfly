/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.interceptor.serverside.secured;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case verifying server-side interceptor execution while accessing an EJB with @RolesAllowed, @DenyAll and @PermitAll security annotations.
 * See https://issues.jboss.org/browse/WFLY-6143 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({SecuredBeanTestCase.SetupTask.class, EjbSecurityDomainSetup.class})
public class SecuredBeanTestCase {

    static final int EJB_INVOKED_METHODS_COUNT = 4;

    @ArquillianResource
    private static InitialContext ctx;

    @Deployment
    public static Archive<?> runAsDeployment() {
        final Package currentPackage = SecuredBeanTestCase.class.getPackage();
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        return ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class)
                .addClass(Util.class)
                .addClasses(SampleInterceptor.class)
                .addPackage(AbstractServerInterceptorsSetupTask.class.getPackage())
                .addPackage(SecuredBeanTestCase.class.getPackage())
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage, "roles.properties", "roles.properties")
                .addAsWebInfResource(currentPackage, "jboss-web.xml", "jboss-web.xml")
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF")
                .addAsManifestResource(currentPackage, "permissions.xml", "permissions.xml");
    }

    @Test
    @InSequence(1)
    public void securedBeanAccessCheck() throws NamingException {
        ctx = new InitialContext();
        SecuredBean bean = (SecuredBean) ctx.lookup("java:module/" + SecuredBean.class.getSimpleName());

        bean.permitAll("permitAll");

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
    public void securedBeanRoleCheck() throws Exception {
        final Callable<Void> callable = () -> {
            ctx = new InitialContext();
            SecuredBean bean = (SecuredBean) ctx.lookup("java:module/" + SecuredBean.class.getSimpleName());

            try {
                bean.roleEcho("role1access");
            } catch (EJBAccessException ignored) {
            }

            try {
                bean.role2Echo("access");
            } catch (EJBAccessException e) {
                // expected
            }
            Assert.assertEquals(0, SampleInterceptor.latch.getCount());
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    SampleInterceptor.class,
                    "interceptor-module-secured",
                    "module.xml",
                    SecuredBeanTestCase.class.getResource("module.xml"),
                    "server-side-interceptor-secured.jar"
                    )
            );
        }
    }

}
