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
import java.util.MissingResourceException;
import java.util.Optional;
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
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getResourceAttributeDescription(attributeName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, suffixes);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationDescription(operationName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? List.of(paramName) : List.of(operationName, paramName));
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationParameterDescription(operationName, paramName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? List.of(paramName) : List.of(operationName, paramName), suffixes);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, List.of(operationName, StandardResourceDescriptionResolver.REPLY));
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationReplyDescription(operationName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, List.of(operationName, StandardResourceDescriptionResolver.REPLY), suffixes);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationReplyValueTypeDescription(operationName, locale, bundle, suffixes);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getNotificationDescription(String notificationType, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, notificationType);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getNotificationDescription(notificationType, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, childType);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getChildTypeDescription(childType, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getResourceDescription(locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, ModelDescriptionConstants.DEPRECATED);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getResourceDeprecatedDescription(locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, attributeName, ModelDescriptionConstants.DEPRECATED);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName, ModelDescriptionConstants.DEPRECATED);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationDeprecatedDescription(operationName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        Optional<MissingResourceException> exception = Optional.empty();
        for (PathElement path : this.paths) {
            String key = this.getBundleKey(path, operationName.equals(ModelDescriptionConstants.ADD) ? List.of(paramName) : List.of(operationName, paramName), ModelDescriptionConstants.DEPRECATED);
            try {
                return bundle.getString(key);
            } catch (MissingResourceException e) {
                if (exception.isEmpty()) {
                    exception = Optional.of(e);
                }
            }
        }
        try {
            return this.parent.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
        } catch (MissingResourceException e) {
            throw exception.orElse(e);
        }
    }

    private String getBundleKey(PathElement path) {
        return this.getBundleKey(path, List.of());
    }

    private String getBundleKey(PathElement path, String key, String... suffixes) {
        return this.getBundleKey(path, List.of(key), suffixes);
    }

    private String getBundleKey(PathElement path, List<String> keys, String... suffixes) {
        StringBuilder builder = new StringBuilder(this.prefix);
        for (String value : path.isWildcard() ? List.of(path.getKey()) : List.of(path.getKey(), path.getValue())) {
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
