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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Consolidates common logic when creating {@link org.jboss.as.controller.descriptions.ResourceDescriptionResolver}s for a given subsystem.
 * @author Paul Ferraro
 */
public class SubsystemResourceDescriptionResolver extends StandardResourceDescriptionResolver {

    private static final String RESOURCE_NAME_PATTERN = "%s.LocalDescriptions";

    private final String subsystemName;
    private final List<PathElement> paths;

    protected SubsystemResourceDescriptionResolver(String subsystemName, Class<? extends Extension> extensionClass) {
        this(subsystemName, extensionClass, Collections.<PathElement>emptyList());
    }

    protected SubsystemResourceDescriptionResolver(String subsystemName, Class<? extends Extension> extensionClass, PathElement... paths) {
        this(subsystemName, extensionClass, Arrays.asList(paths));
    }

    protected SubsystemResourceDescriptionResolver(String subsystemName, Class<? extends Extension> extensionClass, List<PathElement> paths) {
        super(paths.isEmpty() ? subsystemName : getBundleKey(subsystemName, paths.get(0)), String.format(RESOURCE_NAME_PATTERN, extensionClass.getPackage().getName()), extensionClass.getClassLoader(), true, false);
        this.subsystemName = subsystemName;
        this.paths = paths;
    }

    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getResourceAttributeDescription(attributeName, locale, bundle);
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
    }

    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, this.isReuseAttributesForAdd() && operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName });
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, this.isReuseAttributesForAdd() && operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName }, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
    }

    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, new String[] { operationName, REPLY });
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationReplyDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, new String[] { operationName, REPLY }, suffixes);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationReplyValueTypeDescription(operationName, locale, bundle, suffixes);
    }

    @Override
    public String getNotificationDescription(String notificationType, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, notificationType);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getNotificationDescription(notificationType, locale, bundle);
    }

    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, childType);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getChildTypeDescription(childType, locale, bundle);
    }

    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getResourceDescription(locale, bundle);
    }

    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getResourceDeprecatedDescription(locale, bundle);
    }

    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
    }

    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationDeprecatedDescription(operationName, locale, bundle);
    }

    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, this.isReuseAttributesForAdd() && operationName.equals(ModelDescriptionConstants.ADD) ? new String[] { paramName } : new String[] { operationName, paramName }, ModelDescriptionConstants.DEPRECATED);
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return super.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
    }

    private String getBundleKey(PathElement path) {
        return this.getBundleKey(path, new String[0]);
    }

    private String getBundleKey(PathElement path, String key, String... suffixes) {
        return this.getBundleKey(path, new String[] { key }, suffixes);
    }

    private String getBundleKey(PathElement path, String[] keys, String... suffixes) {
        return getBundleKey(this.subsystemName, path, keys, suffixes);
    }

    private static String getBundleKey(String subsystemName, PathElement path) {
        return getBundleKey(subsystemName, path, new String[0]);
    }

    private static String getBundleKey(String subsystemName, PathElement path, String[] keys, String... suffixes) {
        StringBuilder builder = new StringBuilder(subsystemName);
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
