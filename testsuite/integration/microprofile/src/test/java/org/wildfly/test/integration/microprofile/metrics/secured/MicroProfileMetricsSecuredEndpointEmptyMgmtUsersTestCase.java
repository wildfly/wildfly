/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.metrics.secured;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileMetricsSecuredEndpointSetupTask.class, EmptyMgmtUsersSetupTask.class})
public class MicroProfileMetricsSecuredEndpointEmptyMgmtUsersTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeElytronProfileEnabled(); // Elytron has different behavior than PicketBox
        // https://issues.jboss.org/browse/WFLY-10861?focusedCommentId=13623626&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13623626
    }

    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "MicroProfileMetricsSecuredEndpointEmptyMgmtUsersTestCase.jar")
                .addClasses(MicroProfileMetricsSecuredEndpointSetupTask.class, EmptyMgmtUsersSetupTask.class);
        return deployment;
    }

    @Test
    public void securedHTTPEndpointWithoutUserDefined() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(endpointURL));
            assertEquals(401, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
        }
    }

}
