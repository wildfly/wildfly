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

package org.jboss.as.clustering.controller.descriptions;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Generates resource descriptions for a child resource of a subsystem.
 * @author Paul Ferraro
 */
public class ChildResourceDescriptionResolver implements ResourceDescriptionResolver {

    private final ResourceDescriptionResolver parent;
    private final String prefix;
    private final List<PathElement> paths;

    protected ChildResourceDescriptionResolver(ResourceDescriptionResolver parent, String prefix, List<PathElement> paths) {
        this.parent = parent;
        this.prefix = prefix;
        this.paths = paths;
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return this.parent.getResourceBundle(locale);
    }

    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getResourceAttributeDescription(attributeName, locale, bundle);
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
    }

    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName });
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationParameterDescription(operationName, paramName, locale, bundle);
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName }, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
    }

    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, new String[] { operationName, StandardResourceDescriptionResolver.REPLY });
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationReplyDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, new String[] { operationName, StandardResourceDescriptionResolver.REPLY }, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationReplyValueTypeDescription(operationName, locale, bundle, suffixes);
    }

    @Override
    public String getNotificationDescription(String notificationType, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, notificationType);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getNotificationDescription(notificationType, locale, bundle);
    }

    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, childType);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getChildTypeDescription(childType, locale, bundle);
    }

    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getResourceDescription(locale, bundle);
    }

    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getResourceDeprecatedDescription(locale, bundle);
    }

    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
    }

    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationDeprecatedDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName }, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return this.parent.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
    }

    private String getBundleKey(PathElement path) {
        return this.getBundleKey(path, new String[0]);
    }

    private String getBundleKey(PathElement path, String key, String... suffixes) {
        return this.getBundleKey(path, new String[] { key }, suffixes);
    }

    private String getBundleKey(PathElement path, String[] keys, String... suffixes) {
        StringBuilder builder = new StringBuilder(this.prefix);
        for (String value : path.isWildcard() ? new String[] { path.getKey() } : path.getKeyValuePair()) {
            builder.append('.').append(value);
        }
        for (String key : keys) {
            builder.append('.').append(key);
        }
        for (String suffix : suffixes) {
            builder.append('.').append(suffix);
        }
        return builder.toString();
    }
}
