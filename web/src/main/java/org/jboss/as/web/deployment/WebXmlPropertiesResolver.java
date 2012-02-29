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
package org.jboss.as.web.deployment;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.server.parsing.PropertiesValueResolver;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.metadata.web.spec.WelcomeFileListMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ErrorPageMetaData;

/**
 *
 * @author kulikov
 */
public class WebXmlPropertiesResolver {
    public static void process(WebMetaData webMetaData) {
        //servlet-mapping
        List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
        if (servletMappings != null) {
            for (ServletMappingMetaData item : servletMappings) {
                item.setUrlPatterns(resolve(item.getUrlPatterns()));
            }
        }

        //welcome file list
        WelcomeFileListMetaData wfl = webMetaData.getWelcomeFileList();
        if (wfl != null) {
            wfl.setWelcomeFiles(resolve(wfl.getWelcomeFiles()));
        }

        //context-param
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        resolveProps(contextParams);

        //filter
        FiltersMetaData filters = webMetaData.getFilters();
        if (filters != null) {
            for (FilterMetaData filter : filters) {
                resolveProps(filter.getInitParam());
            }
        }

        //filter mapping
        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings != null) {
            for (FilterMappingMetaData item : filterMappings) {
                item.setUrlPatterns(resolve(item.getUrlPatterns()));
            }
        }

        //servlet
        ServletsMetaData servlets = webMetaData.getServlets();
        if (servlets != null) {
            for (ServletMetaData servlet : servlets) {
                resolveProps(servlet.getInitParam());
            }
        }

        //error-page
        List<ErrorPageMetaData> errorPages = webMetaData.getErrorPages();
        if (errorPages != null) {
            for (ErrorPageMetaData item : errorPages) {
                item.setLocation(PropertiesValueResolver.replaceProperties(item.getLocation()));
            }
        }

    }

    private static void resolveProps(List<ParamValueMetaData> props) {
        if (props == null) {
            return;
        }

        for (ParamValueMetaData item : props) {
            item.setParamValue(PropertiesValueResolver.replaceProperties(item.getParamValue()));
        }
    }

    private static List<String> resolve(List<String> expressions) {
        ArrayList<String> res = new ArrayList();
        for (String s : expressions) {
            res.add(PropertiesValueResolver.replaceProperties(s));
        }
        return res;
    }
}
