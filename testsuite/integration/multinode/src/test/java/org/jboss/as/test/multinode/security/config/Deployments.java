/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.config;

import java.io.File;

import org.jboss.as.test.multinode.security.api.EJBInfo;
import org.jboss.as.test.multinode.security.api.ServletInfo;
import org.jboss.as.test.multinode.security.util.EJBUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author bmaxwell
 *
 */
public class Deployments {

    private static Logger log = Logger.getLogger(Deployments.class.getName());

    public static Archive<?> createEjbDeployment(EJBInfo ejbInfo) {
        String outputName = ejbInfo.getEjbName() + ".jar";
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, outputName);
        // add ejb package
        jar.addPackage(ejbInfo.getEjbPackage());
        // add ejb api
        jar.addPackage(ejbInfo.getEjbInterface().getPackage());
        // add ejb util
        jar.addPackage(EJBUtil.class.getPackage());
        if(log.isDebugEnabled())
            outputTestDeployment(ejbInfo.getEjbPackage(), jar);
        return jar;
    }

    private static StringAsset JBOSS_WEB_XML = new StringAsset(
            "<?xml version=\"1.0\"?>\n<jboss-web>\n<security-domain>other</security-domain>\n</jboss-web>\n");

    private static StringAsset getWebXmlRoleRequired(String securityRoleRequired) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append(
                "<web-app version=\"3.0\" xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append(
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">");
        sb.append("<security-constraint>\n");
        sb.append("<web-resource-collection>\n");
        sb.append("<web-resource-name>All resources</web-resource-name>\n");
        sb.append("<url-pattern>/*</url-pattern>\n");
        sb.append("</web-resource-collection>\n");
        sb.append("<auth-constraint>\n");
        sb.append(String.format("<role-name>%s</role-name>\n", securityRoleRequired));
        sb.append("</auth-constraint>\n");
        sb.append("</security-constraint>\n");
        sb.append("<security-role>\n");
        sb.append(String.format("<role-name>%s</role-name>\n", securityRoleRequired));
        sb.append("</security-role>\n");
        sb.append("<login-config>\n");
        sb.append("<auth-method>BASIC</auth-method>\n");
        sb.append("<realm-name>Test Logon</realm-name>\n");
        sb.append("</login-config>\n");
        sb.append("</web-app>\n");
        return new StringAsset(sb.toString());
    }

    private static StringAsset getEjbClientXml(boolean excludeLocalReceivers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<jboss-ejb-client xmlns=\"urn:jboss:ejb-client:1.2\">");
        sb.append("<client-context>");
        sb.append("  <ejb-receivers exclude-local-receiver=\"true\">");
        // sb.append(" <ejb-receivers exclude-local-receiver=\"false\">");
        sb.append("    <remoting-ejb-receiver outbound-connection-ref=\"remote-ejb-connection\"/>");
        sb.append("  </ejb-receivers>");
        sb.append("</client-context>");
        sb.append("</jboss-ejb-client>");
        return new StringAsset(sb.toString());
    }

    public static Archive<?> createWarDeployment(String suffix, boolean addJBossEJBClientXml, ServletInfo servletInfo) {
        String outputName = servletInfo.getServletSimpleName() + suffix + ".war";
        WebArchive war = ShrinkWrap.create(WebArchive.class, outputName);
        // add servlet package
        war.addPackage(servletInfo.getServletPackage());
        // add packages needed in the app such as ejb api
        for(Package p : servletInfo.getPackagesRequired())
            war.addPackage(p);
        // add EJBUtil
        war.addPackage(EJBUtil.class.getPackage());

        war.addAsWebInfResource(JBOSS_WEB_XML, "jboss-web.xml");
        war.addAsWebInfResource(getWebXmlRoleRequired(servletInfo.getSecurityRole()), "web.xml");
        if (addJBossEJBClientXml)
            war.addAsWebInfResource(getEjbClientXml(false), "jboss-ejb-client.xml");

        String packageDir = servletInfo.getServletPackage().replaceAll("\\.", "/");
        File outputDir = new File(packageDir + "target/arquillian-deployments/");
        outputDir.mkdirs();
        if(log.isDebugEnabled())
            outputTestDeployment(servletInfo.getServletPackage(), war);
        return war;
    }

    public static void outputTestDeployment(String p, Archive archive) {
        String packageDir = p.replaceAll("\\.", "/");
        File outputDir = new File("target/arquillian-deployments/" + packageDir);
        outputDir.mkdirs();
        archive.as(ZipExporter.class).exportTo(new File(outputDir, archive.getName()), true);
    }

    public static void outputTestDeployment(Package p, Archive archive) {
        String packageDir = p.getName().replaceAll("\\.", "/");
        File outputDir = new File("target/arquillian-deployments/" + packageDir);
        outputDir.mkdirs();
        archive.as(ZipExporter.class).exportTo(new File(outputDir, archive.getName()), true);
    }
    public static void outputTestDeployment(Class testClass, Archive archive) {
        String packageDir = testClass.getPackage().getName().replaceAll("\\.", "/");
        File outputDir = new File("target/arquillian-deployments/" + packageDir);
        outputDir.mkdirs();
        archive.as(ZipExporter.class).exportTo(new File(outputDir, archive.getName()), true);
    }
}