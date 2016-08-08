package org.jboss.as.test.integration.logging.profiles;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jfree.util.Log;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author Petr Kremensky <pkremens@redhat.com>
 */
@ServerSetup(NonExistingProfileTestCase.NonExistingProfileTestCaseSetup.class)
@RunWith(Arquillian.class)
public class NonExistingProfileTestCase {
	@ArquillianResource(LoggingServlet.class)
	URL url;
	private static final String FS = System.getProperty("file.separator");
	private static File loggingTestLog = new File(
			System.getProperty("jbossas.ts.submodule.dir"), "target" + FS
					+ "jbossas" + FS + "standalone" + FS + "log" + FS
					+ "logging-test.log");

	static class NonExistingProfileTestCaseSetup extends
			AbstractMgmtServerSetupTask {

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();
			loggingTestLog.delete();
			// remove LOGGING_TEST from root-logger
			ModelNode op = new ModelNode();
			op.get(OP).set("root-logger-unassign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// remove custom file handler
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("periodic-rotating-file-handler",
					"LOGGING_TEST");
			updates.add(op);

			// we want to perform all operations
			for (ModelNode modelNode : updates) {
				try {
					executeOperation(modelNode);
				} catch (MgmtOperationException exp) {
					Log.warn(exp.getMessage());
				}
			}

		}

		@Override
		protected void doSetup(ManagementClient managementClient)
				throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();

			// add custom file-handler
			ModelNode op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("periodic-rotating-file-handler",
					"LOGGING_TEST");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			ModelNode file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set("logging-test.log");
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			// add handler to root-logger
			op = new ModelNode();
			op.get(OP).set("root-logger-assign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// we want all operations to perform
			for (ModelNode modelNode : updates) {
				try {
					executeOperation(modelNode);
				} catch (MgmtOperationException exp) {
					Log.warn(exp.getMessage());
				}
			}
		}
	}

	@Deployment
	public static WebArchive createDeployment1() {
		WebArchive archive = ShrinkWrap.create(WebArchive.class, "logging.war");
		archive.addClasses(LoggingServlet.class);
		archive.addAsManifestResource(new StringAsset(
				"Logging-Profile: non-existing-profile \n"), "MANIFEST.MF");
		return archive;
	}

	@AfterClass
	@RunAsClient
	public static void cleanCustomFile() {
		loggingTestLog.delete();
	}

	@Test
	@RunAsClient
	public void warningMessageTest() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(loggingTestLog), Charset.forName("UTF-8")));
		String line;
		boolean warningFound = false;
		while ((line = br.readLine()) != null) {
			// Look for message id in order to support all languages
			if (line.contains("JBAS011509")) {
				warningFound = true;
				break;
			}
		}
		br.close();
		Assert.assertTrue(warningFound);
	}

	@Test
	@RunAsClient
	public void defaultLoggingTest() throws IOException {
		// make some logs
		HttpURLConnection http = (HttpURLConnection) new URL(url, "Logger")
				.openConnection();
		int statusCode = http.getResponseCode();
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);
		// check logs
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(loggingTestLog), Charset.forName("UTF-8")));
		String line;
		boolean logFound = false;

		while ((line = br.readLine()) != null) {
			if (line.contains("LoggingServlet is logging")) {
				logFound = true;
				break;
			}
		}
		br.close();
		Assert.assertTrue(logFound);
	}
}