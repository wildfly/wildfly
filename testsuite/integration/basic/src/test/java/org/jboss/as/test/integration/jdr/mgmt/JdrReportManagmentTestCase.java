/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jdr.mgmt;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the JDR Report subsystem management interfaces.
 *
 * @author Mike M. Clark
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JdrReportManagmentTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void generateStandaloneJdrReport() throws Exception {
        // Create the generate-jdr-report operation
        final ModelNode address = new ModelNode();
        address.add("subsystem", "jdr");
        ModelNode operation = Util.getEmptyOperation("generate-jdr-report", address);

        // Execute generate-jdr-report operation
        ModelNode response = managementClient.getControllerClient().execute(operation);
        String outcome = response.get("outcome").asString();
        Assert.assertEquals("JDR Generation failed. Failed response: " + response.asString(), "success", outcome);

        ModelNode result = response.get("result");
        validateJdrTimeStamps(result);

        String location = result.get("report-location").asString();
        Assert.assertNotNull("JDR report location was null", location);

        // Validate report itself.
        File reportFile = new File(location);
        Assert.assertTrue("JDR report missing, not located at " + location, reportFile.exists());
        validateJdrReportContents(reportFile);

        // Clean up report file
        reportFile.delete();
    }

    private void validateJdrReportContents(File reportFile) {
        String reportName = reportFile.getName().replace(".zip", "");

        ZipFile reportZip = null;
        try {
            reportZip = new ZipFile(reportFile);
            validateReportEntries(reportZip, reportName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to validate JDR report: " + reportFile.getName(), e);
        } finally {
            if (reportZip != null) {
                try {
                    reportZip.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close JDR report: " + reportFile.getName(), e);
                }
            }
        }
    }

    private void validateReportEntries(ZipFile reportZip, String reportName) {
        validateEntryNotEmpty("version.txt", reportZip, reportName);
        // TODO: Add additional files for more complete test.
    }

    private void validateJdrTimeStamps(ModelNode result) {
        // TODO: Validate time structures beyond just not null.
        Assert.assertNotNull("JDR start time was null.", result.get("start-time").asString());
        Assert.assertNotNull("JDR end time was null.", result.get("end-time").asString());
    }

    private void validateEntryNotEmpty(String fileName, ZipFile reportZip, String reportName) {
        String entryInZip = reportName + "/" + fileName;
        ZipEntry entry = reportZip.getEntry(entryInZip);
        Assert.assertNotNull("Report entry " + fileName + " missing from JDR report " + reportZip.getName(), entry);
        Assert.assertTrue("Report entry " + fileName + " was empty or could not be determined", entry.getSize() > 0);
    }
}
