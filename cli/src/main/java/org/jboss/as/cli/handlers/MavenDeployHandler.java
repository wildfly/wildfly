/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Deploys a project to AS straight from a Maven pom.xml file.
 *
 * @author Nicholas DiPiazza
 */
public class MavenDeployHandler extends DeployHandler {

    protected final ArgumentWithValue goals;

    public MavenDeployHandler(CommandContext ctx) {
        super(ctx);
        goals = new ArgumentWithValue(this, "--goals");
        goals.addRequiredPreceding(path);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {
        final ModelControllerClient client = ctx.getModelControllerClient();

        String mavenHome = System.getenv().get("MAVEN_HOME");

        if (mavenHome == null) {
            throw new CommandFormatException("In order to use the mavendeploy command, you must set the MAVEN_HOME so that jboss-cli has access to run maven.");
        }

        ParsedCommandLine args = ctx.getParsedCommandLine();
        boolean l = this.l.isPresent(args);
        if (!args.hasProperties() || l) {
            listDeployments(ctx, l);
            return;
        }

        final String path = this.path.getValue(args);
        final File f;
        if (path != null) {
            f = new File(path);
            if (!f.exists()) {
                throw new CommandFormatException("Path " + f.getAbsolutePath() + " doesn't exist.");
            } else if (!isMavenPomFile(f)) {
                throw new CommandFormatException("Path " + f.getAbsolutePath() + " must point to a Maven pom.xml file.");
            }
        } else {
            f = null;
        }

        String goalsStr = goals.getValue(args);
        if (goalsStr == null) {
            goalsStr = "install";
        } else {
            goalsStr = goalsStr.replace("\"", "");
            goalsStr = goalsStr.replace(',', ' ');
        }

        String packaging = null;
        String groupId = null;
        String artifactId = null;
        String name = null;

        try {
            Document mavenDom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            Element projectNode = mavenDom.getDocumentElement();
            if (projectNode == null || !projectNode.getNodeName().equals("project")) {
                throw new CommandFormatException("Invalid maven pom file specified " + f.getAbsolutePath() + " - cannot locate <project> root element.");
            }
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            name = xpath.compile("/project/name").evaluate(mavenDom);
            name = name == null || name.trim().length() == 0 ? null : name;

            groupId = xpath.compile("/project/groupId").evaluate(mavenDom);
            groupId = groupId == null || groupId.trim().length() == 0 ? null : groupId;

            artifactId = xpath.compile("/project/artifactId").evaluate(mavenDom);
            artifactId = artifactId == null || artifactId.trim().length() == 0 ? null : artifactId;

            packaging = xpath.compile("/project/packaging").evaluate(mavenDom);
            packaging = packaging == null || packaging.trim().length() == 0 ? null : packaging;

        } catch (SAXException e) {
            throw new CommandFormatException("Cannot parse " + f.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new CommandFormatException("Cannot parse " + f.getAbsolutePath(), e);
        } catch (ParserConfigurationException e) {
            throw new CommandFormatException("Cannot parse " + f.getAbsolutePath(), e);
        } catch (XPathExpressionException e) {
            throw new CommandFormatException("Cannot parse " + f.getAbsolutePath(), e);
        }

        if (!"war".equals(packaging) && !"ejb".equals(packaging)) {
            throw new CommandFormatException("The mavendeploy command can only deploy war or ejb's.");
        }

        int exitValue = -1;

        try {
            Process mavenProc = Util.isWindows() ? Runtime.getRuntime().exec(mavenHome + "\\bin\\mvn.bat -f " + f.getAbsolutePath() + " " + goalsStr) : Runtime.getRuntime().exec(mavenHome + "/bin/mvn -f " + f.getAbsolutePath() + " " + goalsStr);
            String line = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(mavenProc.getInputStream()));
            while ((line = in.readLine()) != null) {
                ctx.printLine(line);
            }
            in.close();
            exitValue = mavenProc.waitFor();
        } catch (IOException e) {
            throw new CommandFormatException("Failed to run maven on " + f.getAbsolutePath(), e);
        } catch (InterruptedException e) {
            throw new CommandFormatException("Failed to run maven on " + f.getAbsolutePath(), e);
        }
        if (exitValue != 0) {
            throw new CommandFormatException("Maven Build Failed. Deployment will halt.");
        }

        File targetDirectory = new File(f.getParent(), "target");
        if (!targetDirectory.isDirectory()) {
            throw new CommandFormatException("Cannot find target directory. Expected at " + targetDirectory.getAbsolutePath() + ". This is likely because the maven build failed.");
        }
        File resourceToDeploy = new File(targetDirectory, artifactId + "." + packaging);
        if (!resourceToDeploy.isFile()) {
            throw new CommandFormatException("Cannot find newly created resource to deploy. Expected to find this at " + resourceToDeploy.getAbsolutePath() + ". This is likely because the maven build failed.");
        }
        final boolean unmanaged = this.unmanaged.isPresent(args);
        doDepoyFile(ctx, client, args, unmanaged, resourceToDeploy);
    }

    private boolean isMavenPomFile(final File f) {
        if (f.getName().toLowerCase().equals("pom.xml")) {
            return true;
        }
        return false;
    }
}
