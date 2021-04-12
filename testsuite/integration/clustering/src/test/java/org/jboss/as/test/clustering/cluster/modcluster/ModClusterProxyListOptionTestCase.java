/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.modcluster;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URL;

@RunWith(Arquillian.class)
@ServerSetup(ModClusterProxyListOptionTestCase.ServerSetupTask.class)
public class ModClusterProxyListOptionTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = ModClusterProxyListOptionTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";

    public ModClusterProxyListOptionTestCase() {
        super(new String[] { NODE_1, LOAD_BALANCER_1 }, new String[]{DEPLOYMENT_1});
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return deployment();
    }

    private static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(new StringAsset("<h1>Hello World</h1>"), "index.html");
        return war;
    }

    @Test
    public void testProxyList(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL) throws Exception {
        URL lbURL = new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort() + 500,  baseURL.getFile());
        URI lbURI = lbURL.toURI().resolve("index.html");

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            // use timeout to allow for the modcluster registration
            long startTime = System.currentTimeMillis();
            HttpResponse response = null;
            while (System.currentTimeMillis() < startTime + 5000) {
                response = client.execute(new HttpGet(lbURI));
                try {
                    if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
                        break;
                    }
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
            }
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        }
    }

    static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {

            /*
            Set up balancer using deprecated proxy-list option in modcluster subsystem
             */
            this.builder
                    .node(NODE_1)
                    .setup("/subsystem=modcluster/proxy=default:write-attribute(name=proxy-list,value=\"localhost:8590\")")
                    .setup("/subsystem=modcluster/proxy=default:write-attribute(name=status-interval,value=1)")
                    .setup("/subsystem=distributable-web/infinispan-session-management=default/affinity=ranked:add")
                    .teardown("/subsystem=distributable-web/infinispan-session-management=default/affinity=primary-owner:add")
                    .teardown("/subsystem=modcluster/proxy=default:undefine-attribute(name=status-interval)")
                    .teardown("/subsystem=modcluster/proxy=default:undefine-attribute(name=proxy-list)")
            ;
        }
    }
}
