/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jdr.mgmt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the JDR Report subsystem management interfaces.
 *
 * @author Mike M. Clark
 */
@RunAsClient
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals("success", outcome, "JDR Generation failed. Failed response: " + response.asString());

        ModelNode result = response.get("result");
        validateJdrTimeStamps(result);

        String location = result.get("report-location").asString();
        Assertions.assertNotNull(location, "JDR report location was null");

        // Validate report itself.
        File reportFile = new File(location);
        assertTrue(reportFile.exists(), "JDR report missing, not located at " + location);
        validateJdrReportContents(reportFile);

        // Clean up report file
        reportFile.delete();
    }

    /**
     * Tests if jdr.* script files are present in the $JBOSS_HOME/bin directory.
     * The default server copy, see ts.copy-wildfly, does not copy these resources, so we only
     * execute this test when we are testing galleon layers, hence ts.layers enabled.
     */
    @Test
    public void ensureShellScriptExists() {
        Assumptions.assumeTrue(Boolean.getBoolean("ts.layers"), "This test is only executed if we are testing Galleon Layers");

        Path binPath = Paths.get(System.getProperty("jboss.home"), "bin");

        assertTrue(binPath.resolve("jdr.sh").toFile().exists(), "jdr.sh was not found in " + binPath.toString());
        assertTrue(binPath.resolve("jdr.bat").toFile().exists(), "jdr.bat was not found in " + binPath.toString());
        assertTrue(binPath.resolve("jdr.ps1").toFile().exists(), "jdr.ps1 was not found in " + binPath.toString());
    }

    private void validateJdrReportContents(File reportFile) {
        String reportName = reportFile.getName().replace(".zip", "");

        ZipFile reportZip = null;
        try {
            reportZip = new ZipFile(reportFile);
            Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) reportZip.entries();
            Set<String> fileNames = new HashSet<>();
            while (e.hasMoreElements()) {
                String fileName = e.nextElement().toString();
                fileNames.add(fileName);
            }
            validateReportEntries(fileNames, reportName, reportZip);
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

    private void validateReportEntries(Set<String> fileNames, String reportName, ZipFile reportZip) throws IOException {
        ZipEntry zipENtry = validateEntryNotEmpty("product.txt", fileNames, reportName, reportZip);
        String productName = readProductDirectory(reportZip.getInputStream(zipENtry));

        validateEntryNotEmpty("version.txt", fileNames, reportName, reportZip);
        validateEntryNotEmpty("JBOSS_HOME/standalone/configuration/standalone(.*).xml$", "JBOSS_HOME\\\\standalone\\\\configuration\\\\standalone(.*).xml$",
                fileNames, reportName, reportZip);

        validateEntryNotEmpty("sos_strings/" + productName + "/tree.txt", fileNames, reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/jarcheck.txt", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/dump-services.json", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/configuration.json", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/cluster-proxies-configuration.json", fileNames,
                reportName, reportZip);
        validateEntryPresent("sos_strings/" + productName + "/deployment-dependencies.txt", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/jndi-view.json", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/local-module-dependencies.txt", fileNames,
                reportName, reportZip);
        validateEntryNotEmpty("sos_strings/" + productName + "/system-properties.txt", fileNames,
                reportName, reportZip);
        validateEmptyEntry("sos_logs/skips.log", fileNames, reportName, reportZip);
    }

    /**
     * Check if entry (reportname/filename) is presented in reportZip file and is not empty
     *
     * @param fileName   Name of file inside report
     * @param reportZip  Report zip file
     * @param reportName Report root folder name
     * @return The entry found
     */
    private ZipEntry validateEntryNotEmpty(String fileName, Set<String> fileNames,
                                       String reportName, ZipFile reportZip) {
       return validateEntryNotEmpty(fileName, null, fileNames, reportName, reportZip);
    }

    /**
     * Check if entry (reportname/filename) is presented in reportZip file and is not empty
     *
     * @param fileName         Name of file inside report
     * @param fileNameOptional Optional name of file inside report
     * @param reportZip        Report zip file
     * @param reportName       Report root folder name
     * @return The entry found
     */
    private ZipEntry validateEntryNotEmpty(String fileName, String fileNameOptional, Set<String> fileNames,
                                       String reportName, ZipFile reportZip) {
        ZipEntry zipENtry = getZipEntry(reportZip, fileName, fileNameOptional, fileNames, reportName);
        assertTrue(zipENtry.getSize() > 0, "Report entry " + fileName + " was empty or could not be determined");

        return zipENtry;
    }

    /**
     * Check if entry (reportname/filename) is presented in reportZip file, either empty or not empty.
     *
     * @param fileName   Name of file inside report
     * @param reportZip  Report zip file
     * @param reportName Report root folder name
     */
    private void validateEntryPresent(String fileName, Set<String> fileNames,
                                       String reportName, ZipFile reportZip) {
        ZipEntry zipENtry = getZipEntry(reportZip, fileName, null, fileNames, reportName);
        assertNotNull(zipENtry, "Report entry " + fileName + " was not present");
    }

    private ZipEntry getZipEntry(ZipFile reportZip, String fileName, String fileNameOptional, Set<String> fileNames, String reportName) {
        String entryInZip = reportName + "/" + fileName;

        Pattern p = Pattern.compile(entryInZip);
        Optional<String> realFileName = fileNames.stream().filter(name -> p.matcher(name).find()).findFirst();
        if (!realFileName.isPresent() && fileNameOptional != null) {
            entryInZip = reportName + "/" + fileNameOptional;
            Pattern p2 = Pattern.compile(entryInZip);
            realFileName = fileNames.stream().filter(name -> p2.matcher(name).find()).findFirst();
        }

        assertTrue(realFileName.isPresent(), "Report entry " + fileName + " missing from JDR report "
                + reportName);
        return reportZip.getEntry(realFileName.get());
    }

    private void validateJdrTimeStamps(ModelNode result) {
        // TODO: Validate time structures beyond just not null.
        Assertions.assertNotNull(result.get("start-time").asString(), "JDR start time was null.");
        Assertions.assertNotNull(result.get("end-time").asString(), "JDR end time was null.");
    }

    /**
     * Check if entry (reportname/filename) is presented in reportZip file and is empty
     *
     * @param fileName   Name of file inside report
     * @param reportZip  Report zip file
     * @param reportName Report root folder name
     */
    private void validateEmptyEntry(String fileName, Set<String> fileNames, String reportName, ZipFile reportZip) {
        ZipEntry zipEntry = getZipEntry(reportZip, fileName, null, fileNames, reportName);
        assertFalse(zipEntry.getSize() > 0, "Report entry " + fileName + " should be empty");
    }

    private String readProductDirectory(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }
}
