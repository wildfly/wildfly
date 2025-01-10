/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

import org.jboss.dmr.ModelNode;
import org.jboss.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

/**
 * Internal helper creating a shared TLD metadata list based on the domain configuration.
 *
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class SharedTldsMetaDataBuilder {

    public static final AttachmentKey<List<TldMetaData>> ATTACHMENT_KEY = AttachmentKey.create(List.class);

    private static final String[] JSTL_TAGLIBS = {
            "c-1_0-rt.tld",
            "c-1_0.tld",
            "c-1_2.tld",
            "c.tld",
            "fmt-1_0-rt.tld",
            "fmt-1_0.tld",
            "fmt-1_1.tld",
            "fmt.tld",
            "fn-1_1.tld",
            "fn.tld",
            "permittedTaglibs-1_1.tld",
            "permittedTaglibs.tld",
            "scriptfree-1_1.tld",
            "scriptfree.tld",
            "sql-1_0-rt.tld",
            "sql-1_0.tld",
            "sql-1_1.tld",
            "sql.tld",
            "x-1_0-rt.tld",
            "x-1_0.tld",
            "x-1_1.tld",
            "x.tld"
    };

    // Not used right now due to hardcoding
    /** The common container config. */
    private final ModelNode containerConfig;

    public SharedTldsMetaDataBuilder(final ModelNode containerConfig) {
        this.containerConfig = containerConfig;
    }

    public List<TldMetaData> getSharedTlds(DeploymentUnit deploymentUnit) {

        final List<TldMetaData> metadata = new ArrayList<TldMetaData>();

        try {
            ModuleClassLoader jstl = Module.getModuleFromCallerModuleLoader("jakarta.servlet.jstl.api").getClassLoader();
            for (String tld : JSTL_TAGLIBS) {
                InputStream is = jstl.getResourceAsStream("META-INF/" + tld);
                if (is != null) {
                    TldMetaData tldMetaData = parseTLD(is);
                    metadata.add(tldMetaData);
                }
            }
        } catch (ModuleLoadException e) {
            // Ignore
        } catch (Exception e) {
            // Ignore
        }

        List<TldMetaData> additionalSharedTlds = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        if (additionalSharedTlds != null) {
            metadata.addAll(additionalSharedTlds);
        }

        return metadata;
    }

    private TldMetaData parseTLD(final InputStream is) throws Exception {
        try (is) {
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
            return TldMetaDataParser.parse(xmlReader);
        }
    }

}
