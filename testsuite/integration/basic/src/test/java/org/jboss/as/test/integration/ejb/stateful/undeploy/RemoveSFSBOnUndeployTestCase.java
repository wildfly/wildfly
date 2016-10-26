/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.undeploy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class RemoveSFSBOnUndeployTestCase {

    private static final Logger log = Logger.getLogger(RemoveSFSBOnUndeployTestCase.class.getName());

    @ArquillianResource
    Deployer deployer;

    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Deployment(name = "remote", testable = false)
    public static WebArchive createRemoteTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "remote.war")
                .addClasses(TestServlet.class);
    }

    @Deployment(name="ejb", managed = false, testable = false)
    public static JavaArchive createMainTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "ejb.jar")
                .addClasses(TestSfsb.class, TestSfsbRemote.class)
                .addAsManifestResource(new StringAsset("Dependencies: deployment.remote.war\n"), "MANIFEST.MF");

    }
    @Test
    public void testSfsbDestroyedOnUndeploy(
            @ArquillianResource @OperateOnDeployment("remote") URL url) throws IOException,
            ExecutionException, TimeoutException, NamingException {
        deployer.deploy("ejb");
        try {
            final TestSfsbRemote localEcho = (TestSfsbRemote) context.lookup("ejb:/ejb/" + TestSfsb.class.getSimpleName() + "!" + TestSfsbRemote.class.getName()+"?stateful");
            localEcho.invoke();
            assertEquals("PostConstruct", HttpRequest.get(url + "/test", 10, TimeUnit.SECONDS));
            assertEquals("invoke", HttpRequest.get(url + "/test", 10, TimeUnit.SECONDS));
        }finally {
            deployer.undeploy("ejb");
        }
        assertEquals("PreDestroy", HttpRequest.get(url + "/test", 10, TimeUnit.SECONDS));

    }
}
