/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.suspendejb;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;
import javax.naming.InitialContext;

import jakarta.batch.runtime.BatchStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.suspend.SuspendBatchletTestCase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that run as a remote client invoking a remote ejb, which starts a batch
 * job execution.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SuspendBatchletEjbTestCase extends AbstractBatchTestCase {
    private static final String ARCHIVE_NAME = "suspend-batchlet-ejb";
    private static final String JOB_XML_NAME = "suspend-batchlet-ejb.xml";

    @ArquillianResource
    private ManagementClient managementClient;

    @ContainerResource
    private InitialContext remoteContext;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        final WebArchive deployment = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addClasses(TimeoutUtil.class, LongRunningBatchletWithEjb.class,
                        SuspendBatchLocal.class, SuspendBatchRemote.class, SuspendBatchSingleton.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.msc,org.wildfly.security.manager")
                                .exportAsString()))
                .addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "permissions.xml");

        addJobXml(SuspendBatchletEjbTestCase.class.getPackage(), deployment, JOB_XML_NAME);
        return deployment;
    }

    /**
     * A remote java client looks up and invokes a remote ejb, which starts a
     * batch job execution. Then suspend the server, and the batch job execution
     * should be stopped. Resume the server, and the previous batch job execution
     * should be automatically restarted and completed.
     *
     * @throws Exception on any errors
     */
    @Test
    public void testSuspendResumeWithEjb() throws Exception {
        final String lookupName = "ejb:/" + ARCHIVE_NAME + "/"
                + SuspendBatchSingleton.class.getSimpleName() + "!"
                + SuspendBatchRemote.class.getName();
        SuspendBatchRemote suspendRemoteBean = (SuspendBatchRemote) remoteContext.lookup(lookupName);
        suspendRemoteBean.startJob(JOB_XML_NAME, 10);

        Thread.sleep(TimeoutUtil.adjust(200));
        SuspendBatchletTestCase.suspendServer(managementClient);
        Thread.sleep(TimeoutUtil.adjust(500));
        SuspendBatchletTestCase.resumeServer(managementClient);
        Thread.sleep(TimeoutUtil.adjust(500));

        suspendRemoteBean = (SuspendBatchRemote) remoteContext.lookup(lookupName);
        final BatchStatus status = suspendRemoteBean.getStatus();
        Assert.assertEquals(BatchStatus.COMPLETED, status);
    }
}
