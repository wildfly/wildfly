/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * Parse Microcontainer jboss-beans.xml.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentXmlDescriptorParser implements XMLElementReader<ParseResult<KernelDeploymentXmlDescriptor>>, XMLStreamConstants {
    public static final String NAMESPACE = "urn:jboss:mc:7.0";

    private enum Element {
        BEAN(new QName(NAMESPACE, "bean")),
        CLASSLOADER(new QName(NAMESPACE, "classloader")),
        CONSTRUCTOR(new QName(NAMESPACE, "constructor")),
        FACTORY(new QName(NAMESPACE, "factory")),
        PROPERTY(new QName(NAMESPACE, "property")),
        VALUE(new QName(NAMESPACE, "value")),
        INJECT(new QName(NAMESPACE, "inject")),
        VALUE_FACTORY(new QName(NAMESPACE, "value-factory")),
        PARAMETER(new QName(NAMESPACE, "parameter")),
        DEPENDS(new QName(NAMESPACE, "depends")),
        ALIAS(new QName(NAMESPACE, "alias")),
        ANNOTATION(new QName(NAMESPACE, "annotation")),
        CREATE(new QName(NAMESPACE, "create")),
        START(new QName(NAMESPACE, "start")),
        STOP(new QName(NAMESPACE, "stop")),
        DESTROY(new QName(NAMESPACE, "destroy")),
        INSTALL(new QName(NAMESPACE, "install")),
        UNINSTALL(new QName(NAMESPACE, "uninstall")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Element> QNAME_MAP = new HashMap<QName, Element>();

        static {
            for (Element element : Element.values()) {
                QNAME_MAP.put(element.qName, element);
            }
        }

        private Element(final QName qName) {
            this.qName = qName;
        }

        static Element of(QName qName) {
            final Element element = QNAME_MAP.get(qName);
            return element == null ? UNKNOWN : element;
        }
    }

    private enum Attribute {
        MODE(new QName("mode")),
        NAME(new QName("name")),
        TYPE(new QName("type")),
        VALUE(new QName("value")),
        TRIM(new QName("trim")),
        REPLACE(new QName("replace")),
        BEAN(new QName("bean")),
        SERVICE(new QName("service")),
        PROPERTY(new QName("property")),
        CLASS(new QName("class")),
        METHOD(new QName("method")),
        FACTORY_CLASS(new QName("factory-class")),
        FACTORY_METHOD(new QName("factory-method")),
        STATE(new QName("state")),
        TARGET_STATE(new QName("targetState")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Attribute> QNAME_MAP = new HashMap<QName, Attribute>();

        static {
            for (Attribute attribute : Attribute.values()) {
                QNAME_MAP.put(attribute.qName, attribute);
            }
        }

        private Attribute(final QName qName) {
            this.qName = qName;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = QNAME_MAP.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<KernelDeploymentXmlDescriptor> value) throws XMLStreamException {
        final KernelDeploymentXmlDescriptor kernelDeploymentXmlDescriptor = new KernelDeploymentXmlDescriptor();
        final List<BeanMetaDataConfig> beansConfigs = new ArrayList<BeanMetaDataConfig>();
        kernelDeploymentXmlDescriptor.setBeans(beansConfigs);
        value.setResult(kernelDeploymentXmlDescriptor);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final QName attributeName = reader.getAttributeName(i);
            final Attribute attribute = Attribute.of(attributeName);
            final String attributeValue = reader.getAttributeValue(i);
            switch (attribute) {
                case MODE:
                    kernelDeploymentXmlDescriptor.setMode(ModeConfig.of(attributeValue));
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case COMMENT:
                    break;
                case END_ELEMENT:
                    return;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case BEAN:
                            beansConfigs.add(parseBean(reader));
                            break;
                        case UNKNOWN:
                            throw unexpectedElement(reader);
                    }
                    break;
            }
        }
    }

    private BeanMetaDataConfig parseBean(final XMLExtendedStreamReader reader) throws XMLStreamException {
        BeanMetaDataConfig beanConfig = new BeanMetaDataConfig();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case NAME:
                    beanConfig.setName(attributeValue);
                    break;
                case CLASS:
                    beanConfig.setBeanClass(attributeValue);
                    break;
                case MODE:
                    beanConfig.setMode(ModeConfig.of(attributeValue));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case COMMENT:
                    break;
                case END_ELEMENT:
                    return beanConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case ALIAS:
                            Set<String> aliases = beanConfig.getAliases();
                            if (aliases == null) {
                                aliases = new HashSet<String>();
                                beanConfig.setAliases(aliases);
                            }
                            aliases.add(parseAlias(reader));
                            break;
                        case CLASSLOADER:
                            beanConfig.setModule(parseModuleConfig(reader));
                            break;
                        case CONSTRUCTOR:
                            beanConfig.setConstructor(parseConstructor(reader));
                            break;
                        case PROPERTY:
                            Set<PropertyConfig> properties = beanConfig.getProperties();
                            if (properties == null) {
                                properties = new HashSet<PropertyConfig>();
                                beanConfig.setProperties(properties);
                            }
                            properties.add(parseProperty(reader));
                            break;
                        case INSTALL:
                            List<InstallConfig> installs = beanConfig.getInstalls();
                            if (installs == null) {
                                installs = new ArrayList<InstallConfig>();
                                beanConfig.setInstalls(installs);
                            }
                            installs.add(parseInstall(reader));
                            break;
                        case UNINSTALL:
                            List<InstallConfig> uninstalls = beanConfig.getUninstalls();
                            if (uninstalls == null) {
                                uninstalls = new ArrayList<InstallConfig>();
                                beanConfig.setUninstalls(uninstalls);
                            }
                            uninstalls.add(parseInstall(reader));
                            break;
                        case DEPENDS:
                            Set<DependsConfig> depends = beanConfig.getDepends();
                            if (depends == null) {
                                depends = new HashSet<DependsConfig>();
                                beanConfig.setDepends(depends);
                            }
                            depends.add(parseDepends(reader));
                            break;
                        case CREATE:
                            beanConfig.setCreate(parseLifecycle(reader, "create"));
                            break;
                        case START:
                            beanConfig.setStart(parseLifecycle(reader, "start"));
                            break;
                        case STOP:
                            beanConfig.setStop(parseLifecycle(reader, "stop"));
                            break;
                        case DESTROY:
                            beanConfig.setDestroy(parseLifecycle(reader, "destroy"));
                            break;
                        case UNKNOWN:
                            throw unexpectedElement(reader);
                    }
                    break;
            }
        }
        throw unexpectedElement(reader);
    }

    private String parseAlias(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final String alias = parseTextElement(reader);
        if (alias == null || alias.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty alias");
        return alias;
    }

    private ConstructorConfig parseConstructor(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ConstructorConfig ctorConfig = new ConstructorConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case FACTORY_CLASS:
                    ctorConfig.setFactoryClass(attributeValue);
                    break;
                case FACTORY_METHOD:
                    ctorConfig.setFactoryMethod(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        List<ValueConfig> parameters = new ArrayList<ValueConfig>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    ctorConfig.setParameters(parameters.toArray(new ValueConfig[parameters.size()]));
                    return ctorConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case FACTORY:
                            ctorConfig.setFactory(parseInject(reader));
                            break;
                        case PARAMETER:
                            parameters.add(parseParameter(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ModuleConfig parseModuleConfig(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModuleConfig moduleConfig = new ModuleConfig();
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case NAME:
                    moduleConfig.setModuleName(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }
        return moduleConfig;
    }

    private PropertyConfig parseProperty(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final PropertyConfig property = new PropertyConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case NAME:
                    property.setPropertyName(attributeValue);
                    break;
                case CLASS:
                    property.setType(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return property;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case VALUE:
                            property.setValue(parseValue(reader));
                            break;
                        case INJECT:
                            property.setValue(parseInject(reader));
                            break;
                        case VALUE_FACTORY:
                            property.setValue(parseValueFactory(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private InstallConfig parseInstall(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final InstallConfig installConfig = new InstallConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.METHOD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case STATE:
                    installConfig.setWhenRequired(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                case TARGET_STATE:
                    installConfig.setDependencyState(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                case BEAN:
                    installConfig.setDependency(attributeValue);
                    break;
                case METHOD:
                    installConfig.setMethodName(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }

        List<ValueConfig> parameters = new ArrayList<ValueConfig>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    installConfig.setParameters(parameters.toArray(new ValueConfig[parameters.size()]));
                    return installConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case PARAMETER:
                            parameters.add(parseParameter(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private DependsConfig parseDepends(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final DependsConfig dependsConfig = new DependsConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case STATE:
                    dependsConfig.setWhenRequired(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                case TARGET_STATE:
                    dependsConfig.setDependencyState(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                case SERVICE:
                    dependsConfig.setService(Boolean.parseBoolean(attributeValue));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        String dependency = parseTextElement(reader);
        if (dependency == null || dependency.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty dependency");
        dependsConfig.setDependency(dependency);

        return dependsConfig;
    }

    private LifecycleConfig parseLifecycle(final XMLExtendedStreamReader reader, String defaultMethodName) throws XMLStreamException {
        final LifecycleConfig lifecycleConfig = new LifecycleConfig();
        lifecycleConfig.setMethodName(defaultMethodName); // set default
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case METHOD:
                    lifecycleConfig.setMethodName(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        List<ValueConfig> parameters = new ArrayList<ValueConfig>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    lifecycleConfig.setParameters(parameters.toArray(new ValueConfig[parameters.size()]));
                    return lifecycleConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case PARAMETER:
                            parameters.add(parseParameter(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig parseParameter(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ValueConfig valueConfig = null;
        String type = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case CLASS:
                    type = attributeValue;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    if (valueConfig == null)
                        throw new XMLStreamException("Missing value", reader.getLocation());
                    if (valueConfig.getType() == null)
                        valueConfig.setType(type);
                    return valueConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case VALUE:
                            valueConfig = parseValue(reader);
                            break;
                        case INJECT:
                            valueConfig = parseInject(reader);
                            break;
                        case VALUE_FACTORY:
                            valueConfig = parseValueFactory(reader);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private InjectedValueConfig parseInject(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final InjectedValueConfig injectedValueConfig = new InjectedValueConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case BEAN:
                    injectedValueConfig.setBean(attributeValue);
                    break;
                case STATE:
                    injectedValueConfig.setState(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                case SERVICE:
                    injectedValueConfig.setService(attributeValue);
                    break;
                case PROPERTY:
                    injectedValueConfig.setProperty(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return injectedValueConfig;
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueFactoryConfig parseValueFactory(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ValueFactoryConfig valueFactoryConfig = new ValueFactoryConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.BEAN, Attribute.METHOD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case BEAN:
                    valueFactoryConfig.setBean(attributeValue);
                    break;
                case METHOD:
                    valueFactoryConfig.setMethod(attributeValue);
                    break;
                case STATE:
                    valueFactoryConfig.setState(BeanState.valueOf(attributeValue.toUpperCase()));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }

        List<ValueConfig> parameters = new ArrayList<ValueConfig>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    valueFactoryConfig.setParameters(parameters.toArray(new ValueConfig[parameters.size()]));
                    return valueFactoryConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getName())) {
                        case PARAMETER:
                            parameters.add(parseParameter(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig parseValue(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final StringValueConfig valueConfig = new StringValueConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case CLASS:
                    valueConfig.setType(attributeValue);
                    break;
                case REPLACE:
                    valueConfig.setReplaceProperties(Boolean.parseBoolean(attributeValue));
                    break;
                case TRIM:
                    valueConfig.setTrim(Boolean.parseBoolean(attributeValue));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        valueConfig.setValue(parseTextElement(reader));
        return valueConfig;
    }

    private String parseTextElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String value = null;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return value;
                case CHARACTERS:
                    value = reader.getText();
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
        throw unexpectedElement(reader);
    }
}