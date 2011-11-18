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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.jdr.JdrReportDescriptions;
import org.jboss.dmr.ModelNode;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the JDR Report subsystem management interfaces.
 * 
 * @author Mike M. Clark
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class JdrReportManagmentTestCase {

	private static ModelControllerClient modelControllerClient = null;
	
	@BeforeClass
	public static void connectModelControllerClient() {
		final String host = "localhost";
		final int port = 9999;
		
		try {
			modelControllerClient = ModelControllerClient.Factory.create(host, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Cannot create model controller client for host, " + host + " and port " + port, e);
		}
	}
	
	@AfterClass
	public static void closeModelControllerClient() {
		try {
			modelControllerClient.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to close model controller client.", e);
		}
	}
	
	@Test
	public void generateStandaloneJdrReport() throws Exception {
		// Create the generate-jdr-report operation
		final ModelNode address = new ModelNode();
		address.add("subsystem", "jdr");
		ModelNode operation = Util.getEmptyOperation("generate-jdr-report", address);
		
		// Execute generate-jdr-report operation
		ModelNode response = modelControllerClient.execute(operation);
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
		
		// Validate md5 file
		String md5FileName = location + ".md5";
		File md5File = new File(md5FileName);
		Assert.assertTrue("JDR md5 file missing, not located at " + md5FileName, md5File.exists());
		validateJdrMd5(reportFile, md5File);
		
		// Clean up report files
		reportFile.delete();
		md5File.delete();
	}

    private void validateJdrMd5(File reportFile, File md5File) throws Exception {
        String md5FromMd5File = readMd5File(md5File);
        Assert.assertNotNull("MD5 file contents null", md5FromMd5File);
        Assert.assertEquals("MD5 file contents wrong size", md5FromMd5File.length(), 32);
        
        String md5OfReportArchive = getMd5(reportFile);
        Assert.assertEquals("JDR Report has incorrect checksum", md5FromMd5File, md5OfReportArchive);
	}
    
    private String getMd5(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        String md5 = DigestUtils.md5Hex(in);
        return md5;
    }
    
    private String readMd5File(File file) throws Exception {
        String md5 = null;
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        try {
            md5 = bufferedReader.readLine();
        } finally {
            bufferedReader.close();
        }
        
        return md5;
    }

	private void validateJdrReportContents(File reportFile) {
	    String reportName = reportFile.getName().replace(".zip","");
	    
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