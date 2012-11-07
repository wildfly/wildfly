/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.descriptions;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Resource description resovler that does no resolving at all.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class NonResolvingResourceDescriptionResolver implements ResourceDescriptionResolver {
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return key;
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(new HashSet<String>());
            }
        };
    }

    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        return "description";
    }

    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        return attributeName;
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return attributeName;
    }

    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return operationName;
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        return operationName + "-" + paramName;
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return operationName + "-" + paramName;
    }

    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return null;
    }

    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return operationName;
    }

    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        return childType;
    }

    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        return "resource.deprecated";
    }

    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        return attributeName;
    }

    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return operationName;
    }

    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        return operationName;
    }
}
