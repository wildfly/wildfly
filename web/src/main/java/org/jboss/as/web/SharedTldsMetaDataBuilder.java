/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.deployment.JsfVersionMarker;

import org.jboss.dmr.ModelNode;
import org.jboss.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Internal helper creating a shared TLD metadata list based on the domain configuration.
 *
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class SharedTldsMetaDataBuilder {

    private static final String[] JSF_TAGLIBS = { "html_basic.tld", "jsf_core.tld", "mojarra_ext.tld" };
    private static final String[] JSTL_TAGLIBS = { "c-1_0-rt.tld", "c-1_0.tld", "c.tld", "fmt-1_0-rt.tld", "fmt-1_0.tld", "fmt.tld", "fn.tld", "permittedTaglibs.tld", "scriptfree.tld", "sql-1_0-rt.tld", "sql-1_0.tld", "sql.tld", "x-1_0-rt.tld", "x-1_0.tld", "x.tld" };

    private final ArrayList<TldMetaData> jsfTlds = new ArrayList<TldMetaData>();
    private final ArrayList<TldMetaData> jstlTlds = new ArrayList<TldMetaData>();

    // Not used right now due to hardcoding
    /** The common container config. */
    //private final ModelNode containerConfig;

    SharedTldsMetaDataBuilder(final ModelNode containerConfig) {
        //this.containerConfig = containerConfig;
        init();
    }

    private void init() {
        try {
            ModuleClassLoader jsf = Module.getModuleFromCallerModuleLoader(ModuleIdentifier.create("com.sun.jsf-impl")).getClassLoader();
            for (String tld : JSF_TAGLIBS) {
                InputStream is = jsf.getResourceAsStream("META-INF/" + tld);
                if (is != null) {
                    TldMetaData tldMetaData = parseTLD(tld, is);
                    jsfTlds.add(tldMetaData);
                }
            }
        } catch (ModuleLoadException e) {
            // Ignore
        } catch (Exception e) {
            // Ignore
        }
        try {
            ModuleClassLoader jstl = Module.getModuleFromCallerModuleLoader(ModuleIdentifier.create("javax.servlet.jstl.api")).getClassLoader();
            for (String tld : JSTL_TAGLIBS) {
                InputStream is = jstl.getResourceAsStream("META-INF/" + tld);
                if (is != null) {
                    TldMetaData tldMetaData = parseTLD(tld, is);
                    jstlTlds.add(tldMetaData);
                }
            }
        } catch (ModuleLoadException e) {
            // Ignore
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<TldMetaData> getSharedTlds(DeploymentUnit deploymentUnit) {
        final List<TldMetaData> metadata = new ArrayList<TldMetaData>();

        if (!JsfVersionMarker.getVersion(deploymentUnit).equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) {
            metadata.addAll(jsfTlds);
        }

        metadata.addAll(jstlTlds);
        return metadata;
    }

    private TldMetaData parseTLD(String tld, InputStream is)
    throws Exception {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
            return TldMetaDataParser.parse(xmlReader);
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
