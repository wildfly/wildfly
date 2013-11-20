/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.annotation.client;

import com.arjuna.mw.wst11.UserBusinessActivity;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.xts.annotation.service.CompensatableService;
import org.jboss.as.test.xts.annotation.service.CompensatableServiceImpl;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class CompensatableTestCase {

    private static final String DEPLOYMENT_NAME = "compensatable-test";

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addClass(CompensatableClient.class)
                .addClass(CompensatableService.class)
                .addClass(CompensatableServiceImpl.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts\n"), "MANIFEST.MF")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));

        return webArchive;
    }

    @Test
    public void testNoTransaction() throws Exception {
        final String deploymentUrl = getDeploymentUrl();
        final CompensatableService compensatableService = CompensatableClient.newInstance(deploymentUrl);

        final boolean isTransactionActive = compensatableService.isTransactionActive();

        Assert.assertEquals(false, isTransactionActive);
    }

    @Test
    public void testActiveTransaction() throws Exception {
        final String deploymentUrl = getDeploymentUrl();
        final CompensatableService compensatableService = CompensatableClient.newInstance(deploymentUrl);

        final UserBusinessActivity userBusinessActivity = UserBusinessActivity.getUserBusinessActivity();

        userBusinessActivity.begin();
        final boolean isTransactionActive = compensatableService.isTransactionActive();
        userBusinessActivity.close();

        Assert.assertEquals(true, isTransactionActive);
    }

    private String getDeploymentUrl() {
        final String baseUrl = managementClient.getWebUri().toString();

        return baseUrl + "/" + DEPLOYMENT_NAME;
    }

}