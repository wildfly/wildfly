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

package org.jboss.as.cmp.processors;

import org.jboss.as.cmp.jdbc.metadata.JDBCApplicationMetaData;
import org.jboss.as.cmp.jdbc.metadata.parser.JDBCMetaDataParser;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.modules.Module;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * @author John Bailey
 */
public class CmpParsingProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!CmpDeploymentMarker.isCmpDeployment(deploymentUnit)) {
            return;
        }
        final EjbJarMetaData jarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (jarMetaData == null || jarMetaData.getEnterpriseBeans() == null) {
            throw new IllegalStateException("Deployment " + deploymentUnit + " illegally marked as a CMP deployment");
        }

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setXMLResolver(NoopXMLResolver.create());
        XMLStreamReader xmlReader = null;


        // 1.  Create the initial JDBC App
        JDBCApplicationMetaData jdbcMetaData = new JDBCApplicationMetaData(jarMetaData, module.getClassLoader());

        // 2.  Merge in the defaults from standardjbosscmp-jdbc.xml
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream("standardjbosscmp-jdbc.xml");
            xmlReader = inputFactory.createXMLStreamReader(inputStream);
            jdbcMetaData = JDBCMetaDataParser.parse(xmlReader, jdbcMetaData);
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException("Failed to parse 'standardjbosscmp-jdbc.xml'", e);
        } finally {
            VFSUtils.safeClose(inputStream);
        }

        // 3.  Merge in the app provided from jbosscmp-jdbc.xml
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT).getRoot();
        // Locate the descriptor
        final VirtualFile descriptor = deploymentRoot.getChild("META-INF/jbosscmp-jdbc.xml");
        JDBCApplicationMetaData deploymentJdbcApplicationMetaData = null;
        if (descriptor != null && descriptor.exists()) {
            try {
                inputStream = descriptor.openStream();
                xmlReader = inputFactory.createXMLStreamReader(inputStream);
                jdbcMetaData = JDBCMetaDataParser.parse(xmlReader, jdbcMetaData);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to parse jbosscmp-jdbc.xml: " + descriptor.getPathName(), e);
            } finally {
                VFSUtils.safeClose(inputStream);
            }
        }


        deploymentUnit.putAttachment(Attachments.JDBC_APPLICATION_KEY, jdbcMetaData);
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.JDBC_APPLICATION_KEY);
    }
}
