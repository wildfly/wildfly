/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.logging.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.logging.util.LoggingBean;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Petr Křemenský <pkremens@redhat.com>
 */

@RunWith(Arquillian.class)
public class LoggingPreferencesTestCase {

	@Inject
	private LoggingBean loggingBean;

	private static final String FS = System.getProperty("file.separator");
	private static final File logDir = new File(
			System.getProperty("jbossas.ts.submodule.dir"), "target" + FS
					+ "jbossas" + FS + "standalone" + FS + "log");

	private static final File logFile = new File(logDir,
			"jboss-logging-properties-test.log");

	@Deployment
	public static JavaArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
		jar.addClasses(LoggingBean.class);
		jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
		jar.addAsResource(LoggingBean.class.getPackage(),
				"jboss-logging.properties", "META-INF/jboss-logging.properties");
		jar.setManifest(new StringAsset(
				Descriptors.create(ManifestDescriptor.class)
						.attribute("Logging-Profile", "non-existing-profile")
						.exportAsString()));
		return jar;
	}

	@Before
	public void makeLog() {
		loggingBean.log();
	}

	@After
	public void cleanUp() {
		logFile.delete();
	}

	@Test
	public void logsTest() throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					logFile), StandardCharsets.UTF_8));
		} catch (FileNotFoundException ex) {
			Assert.fail("Log file specified in per-deploy configuration was not found");
		}
		;
		String line;
		boolean trace = false;
		boolean fatal = false;
		String traceLine = "JBoss logging bean - trace";
		String fatalLine = "JBoss logging bean - fatal";
		while ((line = br.readLine()) != null) {
			if (line.contains(traceLine)) {
				trace = true;
			}
			if (line.contains(fatalLine)) {
				fatal = true;
			}
			lines.add(line);
		}
		br.close();
		Assert.assertTrue("Log file should contain line: " + traceLine, trace);
		Assert.assertTrue("Log file should contain line: " + fatalLine, fatal);
	}
}
