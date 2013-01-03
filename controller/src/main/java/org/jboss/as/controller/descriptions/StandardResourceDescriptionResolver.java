/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPRECATED;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * {@link ResourceBundle} based {@link ResourceDescriptionResolver} that builds resource bundle
 * keys by taking a "key prefix" provided to the constructor and dot-appending the string params to the
 * various {@code getXXXDescription} methods. The "key prefix" is associated with a particular resource and serves
 * to provide a namespace for the resource's key's in the set of keys used by a {@link ResourceBundle}.
 * <p>
 * Code that uses this class to localize text descriptions should store their text description in a properties
 * file whose keys follow the following format, where "prefix" is the {@code keyPrefix} param passed to the constructor,
 * "attribute-name" is the name of an attribute, "operation-name" is the name of an operation, "param-name" is the
 * name of a parameter to an operation, "child-type" is the name of one of the resource's valid child types, and
 * "value-type-suffix" is the name of some detail element in a parameter, attribute or operation reply value that has a
 * complex type.
 * </p>
 * <p>
 * prefix=The description of the resource
 * prefix.attribute-name=The description of one of the resource's attributes.
 * prefix.attribute-name.value-type-suffix=The description of an element in a complex attribute's {@link ModelDescriptionConstants#VALUE_TYPE}.
 * prefix.operation-name=The description of one of the resource's operations.
 * prefix.operation-name.param-name=The description of one of an operation's parameters.
 * prefix.operation-name.param-name.value-type-suffix=The description of an element in a complex operation parameter's {@link ModelDescriptionConstants#VALUE_TYPE}.
 * prefix.operation-name.reply=The description of an operation's reply value.
 * prefix.operation-name.reply.value-type-suffix=The description of an element in a complex operation reply value's {@link ModelDescriptionConstants#VALUE_TYPE}.
 * prefix.child-type=The description of one of the resource's child resource types.
 * </p>
 * <p>
 * The constructor supports two settings designed to help minimize the need for redundant entries in the properties file:
 * <ol>
 *     <li>{@code reuseAttributesForAdd} affects how the {@code getOperationParameter...} methods work. If {@code true},
 *     the assumption is that for an operation named "add" the text description of a parameter will be the same as
 *     the description of an attribute of the same name. This would allow the properties for this example resource:
 *     <p>
 *         pool.min-size=The minimum pool size.
 *         pool.max-size=The maximum pool size.
 *         pool.add.min-size=The minimum pool size.
 *         pool.add.max-size=The maximum pool size.
 *     </p>
 *     <p>To be reduced to:</p>
 *     <p>
 *         pool.min-size=The minimum pool size.
 *         pool.max-size=The maximum pool size.
 *     </p>
 *
 *     </li>
 *
 *     <li>{@code useUnprefixedChildTypes} affects how the {@link #getChildTypeDescription(String, Locale, ResourceBundle)}
 *     method works. The descriptions of a set of related resources need to include a description in the parent resource
 *     of its relationship to the child resource, as well as the description of the child resource itself. These two
 *     descriptions are often included in the same properties file and may be the exact same text. If  {@code useUnprefixedChildTypes}
 *     is {@code true}, {@code getChildTypeDescription(...)} will assume there is an entry in the properties file
 *     that exactly matches the name of the child type. This would allow the properties for this example set of resources:
 *     <p>
 *         subsystem=The foo subsystem.
 *         ... attributes and operations of the "subsystem" resource
 *         subsystem.connector=A connector that can be used to access the foo.
 *         connector=A connector that can be used to access the foo.
 *         ... attributes and operations of the "connector" resource
 *     </p>
 *     <p>To be reduced to:</p>
 *     <p>
 *         subsystem=The foo subsystem.
 *         ... attributes and operations of the "subsystem" resource
 *         connector=A connector that can be used to access the foo.
 *         connector=A connector that can be used to access the foo.
 *         ... attributes and operations of the "connector" resource
 *     </p>
 *     <p>Note that while this kind of usage is convenient, it often results in slightly lower quality descriptions. For example,
 *     in the example above, a better description for "subsystem.connector" is "The connectors that can be used to access the foo."</p>
 *     </li>
 * </ol>
 * </p>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StandardResourceDescriptionResolver implements ResourceDescriptionResolver {

    /**
     * Additional string dot-appended to the key by the
     * {@link StandardResourceDescriptionResolver#getOperationReplyDescription(String, Locale, ResourceBundle)} method.
     */
    public static final String REPLY = "reply";

    private final String keyPrefix;
    private final String bundleBaseName;
    private final WeakReference<ClassLoader> bundleLoader;
    private final boolean reuseAttributesForAdd;
    private final boolean useUnprefixedChildTypes;

    public StandardResourceDescriptionResolver(final String keyPrefix,
                                               final String bundleBaseName,
                                               final ClassLoader bundleLoader) {
        this(keyPrefix, bundleBaseName, bundleLoader, false, false);
    }

    public StandardResourceDescriptionResolver(final String keyPrefix,
                                               final String bundleBaseName,
                                               final ClassLoader bundleLoader,
                                               final boolean reuseAttributesForAdd,
                                               final boolean useUnprefixedChildTypes) {
        this.keyPrefix = keyPrefix;
        this.bundleBaseName = bundleBaseName;
        this.bundleLoader = new WeakReference<ClassLoader>(bundleLoader);
        this.reuseAttributesForAdd = reuseAttributesForAdd;
        this.useUnprefixedChildTypes = useUnprefixedChildTypes;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public boolean isReuseAttributesForAdd() {
        return reuseAttributesForAdd;
    }

    public boolean isUseUnprefixedChildTypes() {
        return useUnprefixedChildTypes;
    }

    public StandardResourceDescriptionResolver getChildResolver(String key){
        return new StandardResourceDescriptionResolver(keyPrefix+"."+key,bundleBaseName,bundleLoader.get(),reuseAttributesForAdd,useUnprefixedChildTypes);
    }

    /** {@inheritDoc} */
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(bundleBaseName, locale, bundleLoader.get());
    }

    /** {@inheritDoc} */
    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey());
    }

    /** {@inheritDoc} */
    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey(attributeName));
    }

    /** {@inheritDoc} */
    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return bundle.getString(getVariableBundleKey(new String[]{attributeName}, suffixes));
    }

    /** {@inheritDoc} */
    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey(operationName));
    }

    /** {@inheritDoc} */
    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        if (reuseAttributesForAdd && ADD.equals(operationName)) {
            return bundle.getString(getBundleKey(paramName));
        }
        return bundle.getString(getBundleKey(operationName, paramName));
    }

    /** {@inheritDoc} */
    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        String[] fixed;
        if (reuseAttributesForAdd && ADD.equals(operationName)) {
            fixed = new String[]{paramName};
        } else {
            fixed = new String[]{operationName, paramName};
        }
        return bundle.getString(getVariableBundleKey(fixed, suffixes));
    }

    /** {@inheritDoc} */
    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        try {
            return bundle.getString(getBundleKey(operationName, REPLY));
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        try {
            return bundle.getString(getVariableBundleKey(new String[] {operationName, REPLY}, suffixes));
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        final String bundleKey = useUnprefixedChildTypes ? childType : getBundleKey(childType);
        return bundle.getString(bundleKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceDeprecatedDescription(Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey(DEPRECATED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceAttributeDeprecatedDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey(attributeName, DEPRECATED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperationDeprecatedDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return bundle.getString(getBundleKey(operationName, DEPRECATED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperationParameterDeprecatedDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        if (reuseAttributesForAdd && ADD.equals(operationName)) {
            return bundle.getString(getBundleKey(paramName,DEPRECATED));
        }
        return bundle.getString(getBundleKey(operationName, paramName,DEPRECATED));
    }

    private String getBundleKey(String... args) {
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
