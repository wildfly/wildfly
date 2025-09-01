/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import org.jboss.as.web.common.SharedTldsMetaDataBuilder;
import org.jboss.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.common.xml.XMLInputFactoryUtil;

/**
 * Cache the TLDs for JSF and add them to deployments as needed.
 *
 * @author Stan Silvert
 */
public class JSFSharedTldsProcessor implements DeploymentUnitProcessor {

    private static final String[] JSF_TAGLIBS = { "html_basic.tld", "jsf_core.tld", "mojarra_ext.tld", "myfaces_core.tld", "myfaces_html.tld" };

    public JSFSharedTldsProcessor() {
    }

    private Map<String, List<TldMetaData>> getMap() {
        final Map<String, List<TldMetaData>> jsfTldMap = new HashMap<>();
        JSFModuleIdFactory moduleFactory = JSFModuleIdFactory.getInstance();
        List<String> jsfSlotNames = moduleFactory.getActiveJSFVersions();

        for (String slot : jsfSlotNames) {
            final List<TldMetaData> jsfTlds = new ArrayList<TldMetaData>();
            try {
                ModuleClassLoader jsf = Module.getModuleFromCallerModuleLoader(moduleFactory.getImplModId(slot)).getClassLoader();
                for (String tld : JSF_TAGLIBS) {
                    InputStream is = jsf.getResourceAsStream("META-INF/" + tld);
                    if (is != null) {
                        TldMetaData tldMetaData = parseTLD(is);
                        jsfTlds.add(tldMetaData);
                    }
                }
            } catch (ModuleLoadException e) {
                // Ignore
            } catch (Exception e) {
                // Ignore
            }

            jsfTldMap.put(slot, jsfTlds);
        }
        return jsfTldMap;
    }

    private TldMetaData parseTLD(InputStream is) throws Exception {
        try {
            final XMLInputFactory inputFactory = XMLInputFactoryUtil.create();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
            return TldMetaDataParser.parse(xmlReader    );
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

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }

        String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) {
            return;
        }

        // Add the shared TLDs metadata
        List<TldMetaData> tldsMetaData = deploymentUnit.getAttachment(SharedTldsMetaDataBuilder.ATTACHMENT_KEY);
        if (tldsMetaData == null) tldsMetaData = new ArrayList<TldMetaData>();

        String slot = jsfVersion;
        if (!JSFModuleIdFactory.getInstance().isValidJSFSlot(slot)) {
            slot = JSFModuleIdFactory.getInstance().getDefaultSlot();
        }
        slot = JSFModuleIdFactory.getInstance().computeSlot(slot);

        List<TldMetaData> jsfTlds = this.getMap().get(slot);
        if (jsfTlds != null) tldsMetaData.addAll(jsfTlds);
        deploymentUnit.putAttachment(SharedTldsMetaDataBuilder.ATTACHMENT_KEY, tldsMetaData);
    }
}
