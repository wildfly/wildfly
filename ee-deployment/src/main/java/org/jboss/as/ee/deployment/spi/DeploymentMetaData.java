/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.jboss.as.ee.deployment.spi.DeploymentLogger.ROOT_LOGGER;

/**
 * MetaData to the JBoss deployment plan.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 09-Apr-2004
 */
public final class DeploymentMetaData {

    /** The entry name in the deployment plan archive */
    public static final String ENTRY_NAME = "deployment-plan.xml";

    private String deploymentName;
    private List entryList = new ArrayList();

    public DeploymentMetaData(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public DeploymentMetaData(Document document) {
        init(document);
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    /**
     * Add an entry and return an id for that entry
     */
    public String addEntry(String archiveName, String descriptorName) {
        entryList.add(new Entry(archiveName, descriptorName));

        String entryId = "entry_";
        int count = entryList.size();
        if (count < 100)
            entryId += "0";
        if (count < 10)
            entryId += "0";

        return entryId + count;
    }

    public boolean hasEntry(String archiveName, String descriptorName) {
        return entryList.contains(new Entry(archiveName, descriptorName));
    }

    public List getEntryList() {
        return new ArrayList(entryList);
    }

    public Document getDocument() {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("jboss-deployment-plan");

        root.addElement("deployment-name").addText(deploymentName);

        root.addComment("Note, deployment-entry elements are not used by the DeploymentManager");
        root.addComment("The DeploymentManager relies on the the entry naming convention");

        Iterator it = entryList.iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            Element element = root.addElement("deployment-entry");
            element.addElement("archive-name").addText(entry.archiveName);
            element.addElement("descriptor-name").addText(entry.descriptorName);
        }

        return document;
    }

    public String toXMLString() {
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            StringWriter strWriter = new StringWriter(1024);
            XMLWriter metaWriter = new XMLWriter(strWriter, format);
            metaWriter.write(getDocument());
            metaWriter.close();
            return strWriter.toString();
        } catch (IOException ex) {
            ROOT_LOGGER.cannotTransformDeploymentPlanToXML(ex);
            return null;
        }
    }

    private void init(Document document) {
        Element root = document.getRootElement();
        deploymentName = root.elementTextTrim("deployment-name");
        Iterator it = root.elementIterator("deployment-entry");
        while (it.hasNext()) {
            Element element = (Element) it.next();
            String archiveName = element.elementTextTrim("archive-name");
            String descriptorName = element.elementTextTrim("descriptor-name");
            addEntry(archiveName, descriptorName);
        }
    }

    /**
     * An entry in the deployment plan
     */
    public static class Entry {
        private String archiveName;
        private String descriptorName;

        public Entry(String archiveName, String descriptorName) {
            this.archiveName = archiveName;
            this.descriptorName = (descriptorName != null ? descriptorName : "");
        }

        public String getArchiveName() {
            return archiveName;
        }

        public String getDescriptorName() {
            return descriptorName;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Entry) {
                Entry other = (Entry) obj;
                return archiveName.equals(other.archiveName) && descriptorName.equals(other.descriptorName);

            }
            return false;
        }

        public int hashCode() {
            return new String(archiveName + descriptorName).hashCode();
        }
    }
}
