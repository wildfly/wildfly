/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.deployers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.vfs.VirtualFile;
import org.jboss.ws.api.annotation.EndpointConfig;
import org.jboss.ws.common.configuration.AbstractCommonConfigResolver;
import org.jboss.ws.common.integration.WSConstants;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;

public class ConfigResolver extends AbstractCommonConfigResolver {

    private final ClassInfo epClassInfo;
    private final String className;
    private final String annotationConfigName;
    private final String annotationConfigFile;
    private final String descriptorConfigName;
    private final String descriptorConfigFile;
    private final VirtualFile root;
    private final boolean isWar;

    public ConfigResolver(ClassInfo epClassInfo, JBossWebservicesMetaData jwmd, JBossWebMetaData jbwebmd, VirtualFile root, boolean isWar) {
        this.epClassInfo = epClassInfo;
        this.className = epClassInfo.name().toString();
        List<AnnotationInstance> annotations = epClassInfo.annotations().get(
                DotName.createSimple(EndpointConfig.class.getName()));
        if (annotations != null && !annotations.isEmpty()) {
            AnnotationInstance ann = annotations.get(0);
            AnnotationValue av = ann.value("configName");
            this.annotationConfigName = av != null ? av.asString() : null;
            av = ann.value("configFile");
            this.annotationConfigFile = av != null ? av.asString() : null;
        } else {
            this.annotationConfigName = null;
            this.annotationConfigFile = null;
        }
        String f = null;
        String n = null;
        if (jbwebmd != null && jbwebmd.getContextParams() != null) {
            for (ParamValueMetaData pvmd : jbwebmd.getContextParams()) {
                if (WSConstants.JBOSSWS_CONFIG_NAME.equals(pvmd.getParamName())) {
                    n = pvmd.getParamValue();
                }
                if (WSConstants.JBOSSWS_CONFIG_FILE.equals(pvmd.getParamName())) {
                    f = pvmd.getParamValue();
                }
            }
        }
        this.descriptorConfigFile = f != null ? f : (jwmd != null ? jwmd.getConfigFile() : null);
        this.descriptorConfigName = n != null ? n : (jwmd != null ? jwmd.getConfigName() : null);
        this.root = root;
        this.isWar = isWar;
    }

    @Override
    protected String getEndpointClassName() {
        return className;
    }

    @Override
    protected <T extends Annotation> boolean isEndpointClassAnnotated(Class<T> annotation) {
        return epClassInfo.annotations().containsKey(DotName.createSimple(annotation.getName()));
    }

    @Override
    protected String getEndpointConfigNameFromAnnotation() {
        return annotationConfigName;
    }

    @Override
    protected String getEndpointConfigFileFromAnnotation() {
        return annotationConfigFile;
    }

    @Override
    protected String getEndpointConfigNameOverride() {
        return descriptorConfigName;
    }

    @Override
    protected String getEndpointConfigFileOverride() {
        return descriptorConfigFile;
    }

    @Override
    protected URL getConfigFile(String configFileName) throws IOException {
        return root.getChild(configFileName).asFileURL();
    }

    @Override
    protected URL getDefaultConfigFile(String defaultConfigFileName) {
        URL url = null;
        if (isWar) {
            url = asFileURL(root.getChild("/WEB-INF/classes/" + defaultConfigFileName));
        }
        if (url == null) {
            url = asFileURL(root.getChild("/" + defaultConfigFileName));
        }
        return url;
    }

    private URL asFileURL(VirtualFile vf) {
        URL url = null;
        if (vf != null && vf.exists()) {
            try {
                url = vf.asFileURL();
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        return url;
    }
}
