/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.web;

import java.io.IOException;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.merge.web.jboss.JBossWebMetaDataMerger;
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.osgi.resolver.XBundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Process fragment attachments to a WAB.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Dec-2012
 */
public class WabFragmentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XBundle hostBundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        WarMetaData warMetaData = depUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null || hostBundle == null)
            return;

        XBundle fragment = null;

        // Get attached fragments
        BundleWiring wiring = hostBundle.getBundleRevision().getWiring();
        for (BundleWire wire : wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
            fragment = (XBundle) wire.getRequirer().getBundle();
            break;
        }

        // No attached fragments
        if (fragment == null)
            return;

        // Check if the fragment has a web.xml entry
        URL entry = fragment.getEntry("WEB-INF/web.xml");
        if (entry == null)
            return;

        // Parse the web.xml
        WebMetaData fragmentMetaData = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
            inputFactory.setXMLResolver(dtdInfo);
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(entry.openStream());
            fragmentMetaData = WebMetaDataParser.parse(xmlReader, dtdInfo, SpecDescriptorPropertyReplacement.propertyReplacer(depUnit));
        } catch (XMLStreamException ex) {
            WebLogger.WEB_LOGGER.debugf(ex, "Cannot parse web.xml in fragment: %s", fragment);
        } catch (IOException ex) {
            WebLogger.WEB_LOGGER.debugf(ex, "Cannot parse web.xml in fragment: %s", fragment);
        }

        // Merge additional {@link WebMetaData}
        if (fragmentMetaData != null) {
            warMetaData.setWebMetaData(fragmentMetaData);
            JBossWebMetaData mergedMetaData = new JBossWebMetaData();
            JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
            JBossWebMetaDataMerger.merge(mergedMetaData, metaData, fragmentMetaData);
            warMetaData.setMergedJBossWebMetaData(mergedMetaData);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // do nothing
    }
}
