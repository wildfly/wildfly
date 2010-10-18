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

package org.jboss.as.metadata.parser.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.MessageDestinationMetaDataParser;
import org.jboss.as.metadata.parser.ee.ParamValueMetaDataParser;
import org.jboss.as.metadata.parser.ee.SecurityRoleMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationsMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.WebCommonMetaData;


/**
 * @author Remy Maucherat
 */
public class WebCommonMetaDataParser extends MetaDataElementParser {

    public static boolean parse(XMLStreamReader reader, WebCommonMetaData wmd) throws XMLStreamException {
        // Only look at the current element, no iteration
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case DISTRIBUTABLE:
                wmd.setDistributable(new EmptyMetaData());
                requireNoContent(reader);
                break;
            case CONTEXT_PARAM:
                List<ParamValueMetaData> contextParams = wmd.getContextParams();
                if (contextParams == null) {
                    contextParams = new ArrayList<ParamValueMetaData>();
                    wmd.setContextParams(contextParams);
                }
                contextParams.add(ParamValueMetaDataParser.parse(reader));
                break;
            case FILTER:
                FiltersMetaData filters = wmd.getFilters();
                if (filters == null) {
                    filters = new FiltersMetaData();
                    wmd.setFilters(filters);
                }
                filters.add(FilterMetaDataParser.parse(reader));
                break;
            case FILTER_MAPPING:
                List<FilterMappingMetaData> filterMappings = wmd.getFilterMappings();
                if (filterMappings == null) {
                    filterMappings = new ArrayList<FilterMappingMetaData>();
                    wmd.setFilterMappings(filterMappings);
                }
                filterMappings.add(FilterMappingMetaDataParser.parse(reader));
                break;
            case LISTENER:
                List<ListenerMetaData> listeners = wmd.getListeners();
                if (listeners == null) {
                    listeners = new ArrayList<ListenerMetaData>();
                    wmd.setListeners(listeners);
                }
                listeners.add(ListenerMetaDataParser.parse(reader));
                break;
            case SERVLET:
                ServletsMetaData servlets = wmd.getServlets();
                if (servlets == null) {
                    servlets = new ServletsMetaData();
                    wmd.setServlets(servlets);
                }
                servlets.add(ServletMetaDataParser.parse(reader));
                break;
            case SERVLET_MAPPING:
                List<ServletMappingMetaData> servletMappings = wmd.getServletMappings();
                if (servletMappings == null) {
                    servletMappings = new ArrayList<ServletMappingMetaData>();
                    wmd.setServletMappings(servletMappings);
                }
                servletMappings.add(ServletMappingMetaDataParser.parse(reader));
                break;
            case SESSION_CONFIG:
                wmd.setSessionConfig(SessionConfigMetaDataParser.parse(reader));
                break;
            case MIME_MAPPING:
                List<MimeMappingMetaData> mimeMappings = wmd.getMimeMappings();
                if (mimeMappings == null) {
                    mimeMappings = new ArrayList<MimeMappingMetaData>();
                    wmd.setMimeMappings(mimeMappings);
                }
                mimeMappings.add(MimeMappingMetaDataParser.parse(reader));
                break;
            case WELCOME_FILE_LIST:
                wmd.setWelcomeFileList(WelcomeFileListMetaDataParser.parse(reader));
                break;
            case ERROR_PAGE:
                List<ErrorPageMetaData> errorPages = wmd.getErrorPages();
                if (errorPages == null) {
                    errorPages = new ArrayList<ErrorPageMetaData>();
                    wmd.setErrorPages(errorPages);
                }
                errorPages.add(ErrorPageMetaDataParser.parse(reader));
                break;
            case JSP_CONFIG:
                wmd.setJspConfig(JspConfigMetaDataParser.parse(reader));
                break;
            case SECURITY_CONSTRAINT:
                List<SecurityConstraintMetaData> securityConstraints = wmd.getSecurityConstraints();
                if (securityConstraints == null) {
                    securityConstraints = new ArrayList<SecurityConstraintMetaData>();
                    wmd.setSecurityConstraints(securityConstraints);
                }
                securityConstraints.add(SecurityConstraintMetaDataParser.parse(reader));
                break;
            case LOGIN_CONFIG:
                wmd.setLoginConfig(LoginConfigMetaDataParser.parse(reader));
                break;
            case SECURITY_ROLE:
                SecurityRolesMetaData securityRoles = wmd.getSecurityRoles();
                if (securityRoles == null) {
                    securityRoles = new SecurityRolesMetaData();
                    wmd.setSecurityRoles(securityRoles);
                }
                securityRoles.add(SecurityRoleMetaDataParser.parse(reader));
                break;
            case MESSAGE_DESTINATION:
                MessageDestinationsMetaData messageDestinations = wmd.getMessageDestinations();
                if (messageDestinations == null) {
                    messageDestinations = new MessageDestinationsMetaData();
                    wmd.setMessageDestinations(messageDestinations);
                }
                messageDestinations.add(MessageDestinationMetaDataParser.parse(reader));
                break;
            case LOCALE_ENCODING_MAPPING_LIST:
                wmd.setLocalEncodings(LocaleEncodingsMetaDataParser.parse(reader));
                break;
            default: return false;
        }
        return true;
    }

}
