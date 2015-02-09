/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPRECATED;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Resource resolver that in case that doesn't find deprecated description uses subsystem-name.deprecated key.
 * This is useful when you need to deprecate whole subsystem and don't want to add deprecated key entries for each any every resource / attribute / operation
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
@Deprecated
public class DeprecatedResourceDescriptionResolver extends StandardResourceDescriptionResolver {
    private final String DEPRECATED_KEY;

    public DeprecatedResourceDescriptionResolver(String subsystemName, String keyPrefix, String bundleBaseName, ClassLoader bundleLoader) {
        super(keyPrefix, bundleBaseName, bundleLoader);
        this.DEPRECATED_KEY = subsystemName +"." + DEPRECATED;
    }

    public DeprecatedResourceDescriptionResolver(String subsystemName, String keyPrefix, String bundleBaseName, ClassLoader bundleLoader, boolean reuseAttributesForAdd, boolean useUnprefixedChildTypes) {
        super(keyPrefix, bundleBaseName, bundleLoader, reuseAttributesForAdd, useUnprefixedChildTypes);
        this.DEPRECATED_KEY = subsystemName +"." + DEPRECATED;
    }

    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        if (bundle.containsKey(getBundleKey(operationName, DEPRECATED))) {
            return super.getOperationDeprecatedDescription(operationName, locale, bundle);
        }
        return bundle.getString(DEPRECATED_KEY);
    }

    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        if (bundle.containsKey(getBundleKey(paramName, DEPRECATED))) {
            return super.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
        }
        return bundle.getString(DEPRECATED_KEY);
    }

    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        if (bundle.containsKey(getBundleKey(DEPRECATED))) {
            return super.getResourceDeprecatedDescription(locale, bundle);
        }
        return bundle.getString(DEPRECATED_KEY);
    }

    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        if (bundle.containsKey(getBundleKey(attributeName, DEPRECATED))) {
            return super.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
        }
        return bundle.getString(DEPRECATED_KEY);
    }
    //this two method should be from StandardResourceDescriptionResolver but that creates problems with few other resolves, will be properly done in upstream
    protected String getBundleKey(String... args) {
        return getVariableBundleKey(args);
    }

    private String getVariableBundleKey(String[] fixed, String... variable) {
        StringBuilder sb = new StringBuilder(keyPrefix);
        for (String arg : fixed) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(arg);
        }
        if (variable != null) {
            for (String arg : variable) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(arg);
            }
        }
        return sb.toString();
    }

}
