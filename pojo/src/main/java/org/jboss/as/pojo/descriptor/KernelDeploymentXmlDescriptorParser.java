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

package org.jboss.as.pojo.descriptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.ParseResult;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * Parse Microcontainer jboss-beans.xml.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentXmlDescriptorParser implements XMLElementReader<ParseResult<KernelDeploymentXmlDescriptor>>, XMLStreamConstants {
    public static final String NAMESPACE = "urn:jboss:pojo:7.0";

    private enum Element {
        BEAN("bean"),
        BEAN_FACTORY("bean-factory"),
        CLASSLOADER("classloader"),
        CONSTRUCTOR("constructor"),
        FACTORY("factory"),
        PROPERTY("property"),
        VALUE("value"),
        INJECT("inject"),
        VALUE_FACTORY("value-factory"),
        PARAMETER("parameter"),
        DEPENDS("depends"),
        ALIAS("alias"),
        ANNOTATION("annotation"),
        CREATE("create"),
        START("start"),
        STOP("stop"),
        DESTROY("destroy"),
        INSTALL("install"),
        UNINSTALL("uninstall"),
        INCALLBACK("incallback"),
        UNCALLBACK("uncallback"),
        LIST("list"),
        SET("set"),
        MAP("map"),
        ENTRY("entry"),
        KEY("key"),
        UNKNOWN(null);

        private final String localPart;

        private static final Map<String, Element> QNAME_MAP = new HashMap<String, Element>();

        static {
            for (Element element : Element.values()) {
                QNAME_MAP.put(element.localPart, element);
            }
        }

        private Element(final String localPart) {
            this.localPart = localPart;
        }

        static Element of(String localPart) {
            final Element element = QNAME_MAP.get(localPart);
            return element == null ? UNKNOWN : element;
        }
    }

    private enum Attribute {
        MODE("mode"),
        NAME("name"),
        TYPE("type"),
        VALUE("value"),
        TRIM("trim"),
        REPLACE("replace"),
        BEAN("bean"),
        SERVICE("service"),
        PROPERTY("property"),
        CLASS("class"),
        ELEMENT("elementClass"),
        KEY_ELEMENT("keyClass"),
        VALUE_ELEMENT("valueClass"),
        METHOD("method"),
        IGNORED("ignored"),
        SIGNATURE("signature"),
        FACTORY_CLASS("factory-class"),
        FACTORY_METHOD("factory-method"),
        FACTORY_CLASS_LEGACY("factoryClass"),
        FACTORY_METHOD_LEGACY("factoryMethod"),
        STATE("state"),
        TARGET_STATE("targetState"),
        UNKNOWN(null);

        private final String localPart;

        private static final Map<String, Attribute> QNAME_MAP = new HashMap<String, Attribute>();

        static {
            for (Attribute attribute : Attribute.values()) {
                QNAME_MAP.put(attribute.localPart, attribute);
            }
        }

        private Attribute(final String localPart) {
            this.localPart = localPart;
        }

        static Attribute of(String localPart) {
            final Attribute attribute = QNAME_MAP.get(localPart);
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
            final String attributeName = reader.getAttributeLocalName(i);
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
                    switch (Element.of(reader.getLocalName())) {
                        case BEAN:
                            beansConfigs.add(parseBean(reader));
                            break;
                        case BEAN_FACTORY:
                            BeanMetaDataConfig bean = parseBean(reader);
                            beansConfigs.add(toBeanFactory(bean));
                            kernelDeploymentXmlDescriptor.incrementBeanFactoryCount();
                            break;
                        case UNKNOWN:
                            throw unexpectedElement(reader);
                    }
                    break;
            }
        }
    }

    private BeanMetaDataConfig toBeanFactory(final BeanMetaDataConfig bean) {
        return new BeanFactoryMetaDataConfig(bean);
    }

    private BeanMetaDataConfig parseBean(final XMLExtendedStreamReader reader) throws XMLStreamException {
        BeanMetaDataConfig beanConfig = new BeanMetaDataConfig();

        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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
                    switch (Element.of(reader.getLocalName())) {
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
                        case INCALLBACK:
                            List<CallbackConfig> incallbacks = beanConfig.getIncallbacks();
                            if (incallbacks == null) {
                                incallbacks = new ArrayList<CallbackConfig>();
                                beanConfig.setIncallbacks(incallbacks);
                            }
                            incallbacks.add(parseCallback(reader));
                            break;
                        case UNCALLBACK:
                            List<CallbackConfig> uncallbacks = beanConfig.getUncallbacks();
                            if (uncallbacks == null) {
                                uncallbacks = new ArrayList<CallbackConfig>();
                                beanConfig.setUncallbacks(uncallbacks);
                            }
                            uncallbacks.add(parseCallback(reader));
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
            throw PojoLogger.ROOT_LOGGER.nullOrEmptyAlias();
        return alias;
    }

    private ConstructorConfig parseConstructor(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ConstructorConfig ctorConfig = new ConstructorConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case FACTORY_CLASS:
                case FACTORY_CLASS_LEGACY:
                    ctorConfig.setFactoryClass(attributeValue);
                    break;
                case FACTORY_METHOD:
                case FACTORY_METHOD_LEGACY:
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
                    switch (Element.of(reader.getLocalName())) {
                        case FACTORY:
                            ctorConfig.setFactory(parseFactory(reader));
                            break;
                        case PARAMETER:
                            ValueConfig p = parseParameter(reader);
                            p.setIndex(parameters.size());
                            parameters.add(p);
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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
                    switch (Element.of(reader.getLocalName())) {
                        case VALUE:
                            property.setValue(parseValue(reader));
                            break;
                        case INJECT:
                            property.setValue(parseInject(reader));
                            break;
                        case VALUE_FACTORY:
                            property.setValue(parseValueFactory(reader));
                            break;
                        case LIST:
                            property.setValue(parseList(reader));
                            break;
                        case SET:
                            property.setValue(parseSet(reader));
                            break;
                        case MAP:
                            property.setValue(parseMap(reader));
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case STATE:
                    installConfig.setWhenRequired(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                    break;
                case TARGET_STATE:
                    installConfig.setDependencyState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
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
                    switch (Element.of(reader.getLocalName())) {
                        case PARAMETER:
                            ValueConfig p = parseParameter(reader);
                            p.setIndex(parameters.size());
                            parameters.add(p);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private CallbackConfig parseCallback(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final CallbackConfig callbackConfig = new CallbackConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.METHOD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case STATE:
                    callbackConfig.setWhenRequired(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                    break;
                case TARGET_STATE:
                    callbackConfig.setState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                    break;
                case METHOD:
                    callbackConfig.setMethodName(attributeValue);
                    break;
                case SIGNATURE:
                    callbackConfig.setSignature(attributeValue);
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
                    return callbackConfig;
            }
        }
        throw unexpectedElement(reader);
    }

    private DependsConfig parseDepends(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final DependsConfig dependsConfig = new DependsConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case STATE:
                    dependsConfig.setWhenRequired(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                    break;
                case TARGET_STATE:
                    dependsConfig.setDependencyState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
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
            throw PojoLogger.ROOT_LOGGER.nullOrEmptyDependency();
        dependsConfig.setDependency(dependency);

        return dependsConfig;
    }

    private LifecycleConfig parseLifecycle(final XMLExtendedStreamReader reader, String defaultMethodName) throws XMLStreamException {
        final LifecycleConfig lifecycleConfig = new LifecycleConfig();
        lifecycleConfig.setMethodName(defaultMethodName); // set default
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case METHOD:
                    lifecycleConfig.setMethodName(attributeValue);
                    break;
                case IGNORED:
                    lifecycleConfig.setIgnored(Boolean.parseBoolean(attributeValue));
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
                    switch (Element.of(reader.getLocalName())) {
                        case PARAMETER:
                            ValueConfig p = parseParameter(reader);
                            p.setIndex(parameters.size());
                            parameters.add(p);
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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
                case CHARACTERS:
                    final StringValueConfig svc = new StringValueConfig();
                    svc.setValue(reader.getText());
                    valueConfig = svc;
                    break;
                case END_ELEMENT:
                    if (valueConfig == null)
                        throw new XMLStreamException(PojoLogger.ROOT_LOGGER.missingValue(), reader.getLocation());
                    if (valueConfig.getType() == null)
                        valueConfig.setType(type);
                    return valueConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getLocalName())) {
                        case VALUE:
                            valueConfig = parseValue(reader);
                            break;
                        case INJECT:
                            valueConfig = parseInject(reader);
                            break;
                        case VALUE_FACTORY:
                            valueConfig = parseValueFactory(reader);
                            break;
                        case LIST:
                            valueConfig = parseList(reader);
                            break;
                        case SET:
                            valueConfig = parseSet(reader);
                            break;
                        case MAP:
                            valueConfig = parseMap(reader);
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case BEAN:
                    injectedValueConfig.setBean(attributeValue);
                    break;
                case STATE:
                    injectedValueConfig.setState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
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

    private FactoryConfig parseFactory(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final FactoryConfig factoryConfig = new FactoryConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.BEAN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case BEAN:
                    factoryConfig.setBean(attributeValue);
                    break;
                case STATE:
                    factoryConfig.setState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
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
                    return factoryConfig;
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig parseList(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return parseCollection(reader, new ListConfig());
    }

    private ValueConfig parseSet(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return parseCollection(reader, new SetConfig());
    }

    private ValueConfig parseCollection(final XMLExtendedStreamReader reader, CollectionConfig config) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case CLASS:
                    config.setType(attributeValue);
                    break;
                case ELEMENT:
                    config.setElementType(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return config;
                case START_ELEMENT:
                    switch (Element.of(reader.getLocalName())) {
                        case VALUE:
                            config.addValue(parseValue(reader));
                            break;
                        case INJECT:
                            config.addValue(parseInject(reader));
                            break;
                        case VALUE_FACTORY:
                            config.addValue(parseValueFactory(reader));
                            break;
                        case LIST:
                            config.addValue(parseList(reader));
                            break;
                        case SET:
                            config.addValue(parseSet(reader));
                            break;
                        case MAP:
                            config.addValue(parseMap(reader));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig parseMap(final XMLExtendedStreamReader reader) throws XMLStreamException {
        MapConfig mapConfig = new MapConfig();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
            final String attributeValue = reader.getAttributeValue(i);

            switch (attribute) {
                case CLASS:
                    mapConfig.setType(attributeValue);
                    break;
                case KEY_ELEMENT:
                    mapConfig.setKeyType(attributeValue);
                    break;
                case VALUE_ELEMENT:
                    mapConfig.setValueType(attributeValue);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return mapConfig;
                case START_ELEMENT:
                    switch (Element.of(reader.getLocalName())) {
                        case ENTRY:
                            ValueConfig[] entry = parseEntry(reader);
                            mapConfig.put(entry[0], entry[1]);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig[] parseEntry(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ValueConfig[] entry = new ValueConfig[2];
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    return entry;
                case START_ELEMENT:
                    switch (Element.of(reader.getLocalName())) {
                        case KEY:
                            entry[0] = parseValueValue(reader);
                            break;
                        case VALUE:
                            entry[1] = parseValueValue(reader);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueConfig parseValueValue(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ValueConfig value = null;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case END_ELEMENT:
                    if (value == null)
                        throw new XMLStreamException(PojoLogger.ROOT_LOGGER.missingValue(), reader.getLocation());
                    return value;
                case START_ELEMENT:
                    switch (Element.of(reader.getLocalName())) {
                        case VALUE:
                            value = parseValue(reader);
                            break;
                        case INJECT:
                            value = parseInject(reader);
                            break;
                        case VALUE_FACTORY:
                            value = parseValueFactory(reader);
                            break;
                        case LIST:
                            value = parseList(reader);
                            break;
                        case SET:
                            value = parseSet(reader);
                            break;
                        case MAP:
                            value = parseMap(reader);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                case CHARACTERS:
                    StringValueConfig svc = new StringValueConfig();
                    svc.setValue(reader.getText());
                    value = svc;
                    break;
            }
        }
        throw unexpectedElement(reader);
    }

    private ValueFactoryConfig parseValueFactory(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final ValueFactoryConfig valueFactoryConfig = new ValueFactoryConfig();
        final Set<Attribute> required = EnumSet.of(Attribute.BEAN, Attribute.METHOD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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
                    valueFactoryConfig.setState(BeanState.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
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
                    switch (Element.of(reader.getLocalName())) {
                        case PARAMETER:
                            ValueConfig p = parseParameter(reader);
                            p.setIndex(parameters.size());
                            parameters.add(p);
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
            final Attribute attribute = Attribute.of(reader.getAttributeLocalName(i));
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