/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.Resource;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 *
 * Looks for external TLD's
 *
 * @author Stuart Douglas
 */
public class ExternalTldParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String IMPLICIT_TLD = "implicit.tld";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null || warMetaData.getMergedJBossWebMetaData() == null) {
            return;
        }
        TldsMetaData tldsMetaData = deploymentUnit.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        Map<String, TldMetaData> tlds = tldsMetaData.getTlds();

        Set<String> sharedTldUris = new HashSet<>();
        for(TldMetaData shared : tldsMetaData.getSharedTlds(deploymentUnit)) {
            sharedTldUris.add(shared.getUri());
        }

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        try {
            Iterator<Resource> resources = module.globResources("META-INF/**.tld");
            while (resources.hasNext()) {
                final Resource resource = resources.next();
                //horrible hack
                //we don't want to parse JSF TLD's
                //this would be picked up by the shared tlds check below, but this means we don't
                //waste time re-parsing them
                if(resource.getURL().toString().contains("com/sun/jsf-impl/main")) {
                    continue;
                }

                if(resource.getName().startsWith("META-INF/")) {
                    if(tlds.containsKey(resource.getName())) {
                        continue;
                    }
                    if(resource.getURL().getProtocol().equals("vfs")) {
                        continue;
                    }
                    final TldMetaData value = parseTLD(resource);
                    if(sharedTldUris.contains(value.getUri())) {
                        //don't re-include shared TLD's
                        continue;
                    }
                    String key = "/" + resource.getName();
                    if (!tlds.containsKey(key)) {
                        tlds.put(key, value);
                    }
                    if (!tlds.containsKey(value.getUri())) {
                        tlds.put(value.getUri(), value);
                    }
                    if (value.getListeners() != null) {
                        for (ListenerMetaData l : value.getListeners()) {
                            List<ListenerMetaData> listeners = warMetaData.getMergedJBossWebMetaData().getListeners();
                            if(listeners == null) {
                                warMetaData.getMergedJBossWebMetaData().setListeners(listeners = new ArrayList<ListenerMetaData>());
                            }
                            listeners.add(l);
                        }
                    }
                }

            }
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }


    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    private TldMetaData parseTLD(Resource tld)
            throws DeploymentUnitProcessingException {
        if (IMPLICIT_TLD.equals(tld.getName())) {
            // Implicit TLDs are different from regular TLDs
            return new TldMetaData();
        }
        InputStream is = null;
        try {
            is = tld.openStream();
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
            return TldMetaDataParser.parse(xmlReader);
        } catch (XMLStreamException e) {
            throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(tld.getName(), e.getLocation().getLineNumber(),
                    e.getLocation().getColumnNumber()), e);
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(tld.getName()), e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
