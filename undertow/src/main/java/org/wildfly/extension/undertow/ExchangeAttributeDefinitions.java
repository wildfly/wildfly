/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import io.undertow.attribute.AuthenticationTypeExchangeAttribute;
import io.undertow.attribute.BytesSentAttribute;
import io.undertow.attribute.DateTimeAttribute;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.attribute.HostAndPortAttribute;
import io.undertow.attribute.LocalIPAttribute;
import io.undertow.attribute.LocalPortAttribute;
import io.undertow.attribute.LocalServerNameAttribute;
import io.undertow.attribute.PathParameterAttribute;
import io.undertow.attribute.PredicateContextAttribute;
import io.undertow.attribute.QueryParameterAttribute;
import io.undertow.attribute.QueryStringAttribute;
import io.undertow.attribute.RelativePathAttribute;
import io.undertow.attribute.RemoteHostAttribute;
import io.undertow.attribute.RemoteIPAttribute;
import io.undertow.attribute.RemoteObfuscatedIPAttribute;
import io.undertow.attribute.RemoteUserAttribute;
import io.undertow.attribute.RequestHeaderAttribute;
import io.undertow.attribute.RequestLineAttribute;
import io.undertow.attribute.RequestMethodAttribute;
import io.undertow.attribute.RequestPathAttribute;
import io.undertow.attribute.RequestProtocolAttribute;
import io.undertow.attribute.RequestSchemeAttribute;
import io.undertow.attribute.RequestURLAttribute;
import io.undertow.attribute.ResolvedPathAttribute;
import io.undertow.attribute.ResponseCodeAttribute;
import io.undertow.attribute.ResponseHeaderAttribute;
import io.undertow.attribute.ResponseReasonPhraseAttribute;
import io.undertow.attribute.ResponseTimeAttribute;
import io.undertow.attribute.SecureExchangeAttribute;
import io.undertow.attribute.SecureProtocolAttribute;
import io.undertow.attribute.SslCipherAttribute;
import io.undertow.attribute.SslClientCertAttribute;
import io.undertow.attribute.SslSessionIdAttribute;
import io.undertow.attribute.StoredResponse;
import io.undertow.attribute.ThreadNameAttribute;
import io.undertow.attribute.TransportProtocolAttribute;
import io.undertow.util.HttpString;
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.function.ExceptionBiFunction;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
class ExchangeAttributeDefinitions {

    private static final String KEY_NAME = "key";
    private static final SimpleAttributeDefinitionBuilder KEY_BUILDER = SimpleAttributeDefinitionBuilder.create(KEY_NAME, ModelType.STRING, true);

    private static final Map<AttributeDefinition, ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>> ATTRIBUTE_RESOLVERS =
            new HashMap<>();

    private static final SimpleAttributeDefinitionBuilder KEY_PREFIX_BUILDER = SimpleAttributeDefinitionBuilder.create("key-prefix", ModelType.STRING, true);
    private static final StringListAttributeDefinition NAMES = StringListAttributeDefinition.Builder.of("names")
            .setAllowExpression(true)
            .setAttributeMarshaller(new AttributeMarshaller.AttributeElementMarshaller() {

                @Override
                public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
                    assert attribute instanceof StringListAttributeDefinition;
                    try {
                        final List<String> list = ((StringListAttributeDefinition) attribute).unwrap(ExpressionResolver.SIMPLE, resourceModel);
                        if (list.isEmpty()) {
                            return;
                        }
                        if (resourceModel.hasDefined(attribute.getName())) {
                            for (ModelNode value : resourceModel.get(attribute.getName()).asList()) {
                                writer.writeStartElement(attribute.getXmlName());
                                writer.writeAttribute(VALUE, value.asString());
                                writer.writeEndElement();
                            }
                        }

                    } catch (OperationFailedException e) {
                        throw new XMLStreamException(e);
                    }
                }
            })
            .setAttributeParser(new AttributeParser() {
                @Override
                public boolean isParseAsElement() {
                    return true;
                }

                @Override
                public void parseElement(final AttributeDefinition attribute, final XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException {
                    ParseUtils.requireSingleAttribute(reader, VALUE);
                    final String value = reader.getAttributeValue(0);
                    operation.get(attribute.getName()).add(value);
                    ParseUtils.requireNoContent(reader);
                }
            })
            .setRequired(true)
            .setXmlName("name")
            .build();

    private static final SimpleAttributeDefinition AUTHENTICATION_TYPE_KEY = createKey("authenticationType");
    private static final ObjectTypeAttributeDefinition AUTHENTICATION_TYPE = create(
            ObjectTypeAttributeDefinition.create("authentication-type", AUTHENTICATION_TYPE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(AUTHENTICATION_TYPE_KEY, context, model, AuthenticationTypeExchangeAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition BYTES_SENT_KEY = createKey("bytesSent");
    private static final ObjectTypeAttributeDefinition BYTES_SENT = create(
            ObjectTypeAttributeDefinition.create("bytes-sent", BYTES_SENT_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(BYTES_SENT_KEY, context, model,
                            new BytesSentAttribute(false), new Function<String, Object>() {
                                @Override
                                public Object apply(final String s) {
                                    return Long.valueOf(s);
                                }
                            });
                }
            });

    private static final SimpleAttributeDefinition DATE_TIME_KEY = createKey("dateTime");
    private static final SimpleAttributeDefinition DATE_FORMAT = SimpleAttributeDefinitionBuilder.create("date-format", ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true, true) {
                @Override
                public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
                    super.validateParameter(parameterName, value);
                    try {
                        new SimpleDateFormat(value.asString());
                    } catch (IllegalArgumentException ignore) {
                        throw UndertowLogger.ROOT_LOGGER.invalidDateTimeFormatterPattern(value.asString());
                    }
                }
            })
            .build();
    private static final SimpleAttributeDefinition TIME_ZONE = SimpleAttributeDefinitionBuilder.create("time-zone", ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true, true) {
                private final List<String> zoneIds = Arrays.asList(TimeZone.getAvailableIDs());
                @Override
                public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
                    super.validateParameter(parameterName, value);
                    if (!zoneIds.contains(value.asString())) {
                        throw UndertowLogger.ROOT_LOGGER.invalidTimeZoneId(value.asString());
                    }
                }
            })
            .build();
    private static final ObjectTypeAttributeDefinition DATE_TIME = create(
            ObjectTypeAttributeDefinition.create("date-time", DATE_TIME_KEY, DATE_FORMAT, TIME_ZONE),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final ExchangeAttribute exchangeAttribute;
                    if (model.hasDefined(DATE_FORMAT.getName())) {
                        String timeZone = null;
                        if (model.hasDefined(TIME_ZONE.getName())) {
                            timeZone = TIME_ZONE.resolveModelAttribute(context, model).asString();
                        }
                        exchangeAttribute = new DateTimeAttribute(DATE_FORMAT.resolveModelAttribute(context, model).asString(), timeZone);
                    } else {
                        exchangeAttribute = DateTimeAttribute.INSTANCE;
                    }
                    return createSingleton(DATE_TIME_KEY, context, model, exchangeAttribute);
                }
            });

    private static final SimpleAttributeDefinition HOST_AND_PORT_KEY = createKey("hostAndPort");
    private static final ObjectTypeAttributeDefinition HOST_AND_PORT = create(
            ObjectTypeAttributeDefinition.create("host-and-port", HOST_AND_PORT_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(HOST_AND_PORT_KEY, context, model, HostAndPortAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition LOCAL_IP_KEY = createKey("localIp");
    private static final ObjectTypeAttributeDefinition LOCAL_IP = create(
            ObjectTypeAttributeDefinition.create("local-ip", LOCAL_IP_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(LOCAL_IP_KEY, context, model, LocalIPAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition LOCAL_PORT_KEY = createKey("localPort");
    private static final ObjectTypeAttributeDefinition LOCAL_PORT = create(
            ObjectTypeAttributeDefinition.create("local-port", LOCAL_PORT_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(LOCAL_PORT_KEY, context, model, LocalPortAttribute.INSTANCE, new Function<String, Object>() {
                        @Override
                        public Object apply(final String s) {
                            return Integer.valueOf(s);
                        }
                    });
                }
            });

    private static final SimpleAttributeDefinition LOCAL_SERVER_NAME_KEY = createKey("localServerName");
    private static final ObjectTypeAttributeDefinition LOCAL_SERVER_NAME = create(
            ObjectTypeAttributeDefinition.create("local-server-name", LOCAL_SERVER_NAME_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(LOCAL_SERVER_NAME_KEY, context, model, LocalServerNameAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition PATH_PARAMETER_KEY_PREFIX = KEY_PREFIX_BUILDER.build();
    private static final ObjectTypeAttributeDefinition PATH_PARAMETER = create(ObjectTypeAttributeDefinition.Builder.of("path-parameter",
            PATH_PARAMETER_KEY_PREFIX, NAMES),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final Collection<AccessLogAttribute> result = new ArrayList<>(5);
                    for (ModelNode m : NAMES.resolveModelAttribute(context, model).asList()) {
                        final String name = m.asString();
                        final String keyName = resolveKeyName(PATH_PARAMETER_KEY_PREFIX.resolveModelAttribute(context, model), name);
                        result.add(AccessLogAttribute.of(keyName, new PathParameterAttribute(name)));
                    }
                    return result;
                }
            });

    private static final SimpleAttributeDefinition PREDICATE_KEY_PREFIX = KEY_PREFIX_BUILDER.build();
    private static final ObjectTypeAttributeDefinition PREDICATE = create(ObjectTypeAttributeDefinition.Builder.of("predicate",
            PREDICATE_KEY_PREFIX, NAMES),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final Collection<AccessLogAttribute> result = new ArrayList<>(5);
                    for (ModelNode m : NAMES.resolveModelAttribute(context, model).asList()) {
                        final String predicateName = m.asString();
                        final String keyName = resolveKeyName(PREDICATE_KEY_PREFIX.resolveModelAttribute(context, model), predicateName);
                        result.add(AccessLogAttribute.of(keyName, new PredicateContextAttribute(predicateName)));
                    }
                    return result;
                }
            });

    private static final SimpleAttributeDefinition QUERY_PARAMETER_KEY_PREFIX = KEY_PREFIX_BUILDER.build();
    private static final ObjectTypeAttributeDefinition QUERY_PARAMETER = create(ObjectTypeAttributeDefinition.Builder.of("query-parameter",
            QUERY_PARAMETER_KEY_PREFIX, NAMES),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final Collection<AccessLogAttribute> result = new ArrayList<>(5);
                    for (ModelNode m : NAMES.resolveModelAttribute(context, model).asList()) {
                        final String paramName = m.asString();
                        final String keyName = resolveKeyName(QUERY_PARAMETER_KEY_PREFIX.resolveModelAttribute(context, model), paramName);
                        result.add(AccessLogAttribute.of(keyName, new QueryParameterAttribute(paramName)));
                    }
                    return result;
                }
            });

    private static final SimpleAttributeDefinition QUERY_STRING_KEY = createKey("queryString");
    private static final SimpleAttributeDefinition INCLUDE_QUESTION_MARK = SimpleAttributeDefinitionBuilder
            .create("include-question-mark", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();
    private static final ObjectTypeAttributeDefinition QUERY_STRING = create(
            ObjectTypeAttributeDefinition.create("query-string", QUERY_STRING_KEY, INCLUDE_QUESTION_MARK),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    if (INCLUDE_QUESTION_MARK.resolveModelAttribute(context, model).asBoolean()) {
                        return createSingleton(QUERY_STRING_KEY, context, model, QueryStringAttribute.INSTANCE);
                    }
                    return createSingleton(QUERY_STRING_KEY, context, model, QueryStringAttribute.BARE_INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition RELATIVE_PATH_KEY = createKey("relativePath");
    private static final ObjectTypeAttributeDefinition RELATIVE_PATH = create(
            ObjectTypeAttributeDefinition.create("relative-path", RELATIVE_PATH_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(RELATIVE_PATH_KEY, context, model, RelativePathAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REMOTE_HOST_KEY = createKey("remoteHost");
    private static final ObjectTypeAttributeDefinition REMOTE_HOST = create(
            ObjectTypeAttributeDefinition.create("remote-host", REMOTE_HOST_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REMOTE_HOST_KEY, context, model, RemoteHostAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REMOTE_IP_KEY = createKey("remoteIp");
    private static final SimpleAttributeDefinition OBFUSCATED = SimpleAttributeDefinitionBuilder.create("obfuscated", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();
    private static final ObjectTypeAttributeDefinition REMOTE_IP = create(
            ObjectTypeAttributeDefinition.create("remote-ip", REMOTE_IP_KEY, OBFUSCATED),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    if (OBFUSCATED.resolveModelAttribute(context, model).asBoolean()) {
                        return createSingleton(REMOTE_IP_KEY, context, model, RemoteObfuscatedIPAttribute.INSTANCE);
                    }
                    return createSingleton(REMOTE_IP_KEY, context, model, RemoteIPAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REMOTE_USER_KEY = createKey("remoteUser");
    private static final ObjectTypeAttributeDefinition REMOTE_USER = create(
            ObjectTypeAttributeDefinition.create("remote-user", REMOTE_USER_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REMOTE_USER_KEY, context, model, RemoteUserAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_HEADER_KEY_PREFIX = KEY_PREFIX_BUILDER.build();
    private static final ObjectTypeAttributeDefinition REQUEST_HEADER = create(ObjectTypeAttributeDefinition.Builder.of("request-header",
            REQUEST_HEADER_KEY_PREFIX, NAMES),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final Collection<AccessLogAttribute> result = new ArrayList<>(5);
                    for (ModelNode m : NAMES.resolveModelAttribute(context, model).asList()) {
                        final String name = m.asString();
                        final String keyName = resolveKeyName(REQUEST_HEADER_KEY_PREFIX.resolveModelAttribute(context, model), name);
                        result.add(AccessLogAttribute.of(keyName, new RequestHeaderAttribute(HttpString.tryFromString(name))));
                    }
                    return result;
                }
            });

    private static final SimpleAttributeDefinition REQUEST_LINE_KEY = createKey("requestLine");
    private static final ObjectTypeAttributeDefinition REQUEST_LINE = create(
            ObjectTypeAttributeDefinition.create("request-line", REQUEST_LINE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_LINE_KEY, context, model, RequestLineAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_METHOD_KEY = createKey("requestMethod");
    private static final ObjectTypeAttributeDefinition REQUEST_METHOD = create(
            ObjectTypeAttributeDefinition.create("request-method", REQUEST_METHOD_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_METHOD_KEY, context, model, RequestMethodAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_PATH_KEY = createKey("requestPath");
    private static final ObjectTypeAttributeDefinition REQUEST_PATH = create(
            ObjectTypeAttributeDefinition.create("request-path", REQUEST_PATH_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_PATH_KEY, context, model, RequestPathAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_PROTOCOL_KEY = createKey("requestProtocol");
    private static final ObjectTypeAttributeDefinition REQUEST_PROTOCOL = create(
            ObjectTypeAttributeDefinition.create("request-protocol", REQUEST_PROTOCOL_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_PROTOCOL_KEY, context, model, RequestProtocolAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_SCHEME_KEY = createKey("requestScheme");
    private static final ObjectTypeAttributeDefinition REQUEST_SCHEME = create(
            ObjectTypeAttributeDefinition.create("request-scheme", REQUEST_SCHEME_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_SCHEME_KEY, context, model, RequestSchemeAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition REQUEST_URL_KEY = createKey("requestUrl");
    private static final ObjectTypeAttributeDefinition REQUEST_URL = create(
            ObjectTypeAttributeDefinition.create("request-url", REQUEST_URL_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(REQUEST_URL_KEY, context, model, RequestURLAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition RESOLVED_PATH_KEY = createKey("resolvedPath");
    private static final ObjectTypeAttributeDefinition RESOLVED_PATH = create(
            ObjectTypeAttributeDefinition.create("resolved-path", RESOLVED_PATH_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(RESOLVED_PATH_KEY, context, model, ResolvedPathAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition RESPONSE_CODE_KEY = createKey("responseCode");
    private static final ObjectTypeAttributeDefinition RESPONSE_CODE = create(
            ObjectTypeAttributeDefinition.create("response-code", RESPONSE_CODE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(RESPONSE_CODE_KEY, context, model, ResponseCodeAttribute.INSTANCE, new Function<String, Object>() {
                        @Override
                        public Object apply(final String s) {
                            return Integer.valueOf(s);
                        }
                    });
                }
            });

    private static final SimpleAttributeDefinition RESPONSE_HEADER_KEY_PREFIX = KEY_PREFIX_BUILDER.build();
    private static final ObjectTypeAttributeDefinition RESPONSE_HEADER = create(ObjectTypeAttributeDefinition.Builder.of("response-header",
            RESPONSE_HEADER_KEY_PREFIX, NAMES),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    final Collection<AccessLogAttribute> result = new ArrayList<>(5);
                    for (ModelNode m : NAMES.resolveModelAttribute(context, model).asList()) {
                        final String name = m.asString();
                        final String keyName = resolveKeyName(RESPONSE_HEADER_KEY_PREFIX.resolveModelAttribute(context, model), name);
                        result.add(AccessLogAttribute.of(keyName, new ResponseHeaderAttribute(HttpString.tryFromString(name))));
                    }
                    return result;
                }
            });

    private static final SimpleAttributeDefinition RESPONSE_REASON_PHRASE_KEY = createKey("responseReasonPhrase");
    private static final ObjectTypeAttributeDefinition RESPONSE_REASON_PHRASE = create(
            ObjectTypeAttributeDefinition.create("response-reason-phrase", RESPONSE_REASON_PHRASE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(RESPONSE_REASON_PHRASE_KEY, context, model, ResponseReasonPhraseAttribute.INSTANCE);
                }
            });


    private static final SimpleAttributeDefinition RESPONSE_TIME_KEY = createKey("responseTime");
    private static final SimpleAttributeDefinition TIME_UNIT = new SimpleAttributeDefinitionBuilder("time-unit", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(TimeUnit.MILLISECONDS.name()))
            .setValidator(EnumValidator.create(TimeUnit.class, TimeUnit.SECONDS, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS))
            .build();
    private static final ObjectTypeAttributeDefinition RESPONSE_TIME = create(
            ObjectTypeAttributeDefinition.create("response-time", RESPONSE_TIME_KEY, TIME_UNIT),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(RESPONSE_TIME_KEY, context, model,
                            new ResponseTimeAttribute(TimeUnit.valueOf(TIME_UNIT.resolveModelAttribute(context, model).asString())),
                            new Function<String, Object>() {
                                @Override
                                public Object apply(final String s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    return Long.valueOf(s);
                                }
                            });
                }
            });

    private static final SimpleAttributeDefinition SECURE_EXCHANGE_KEY = createKey("secureExchange");
    private static final ObjectTypeAttributeDefinition SECURE_EXCHANGE = create(
            ObjectTypeAttributeDefinition.create("secure-exchange", SECURE_EXCHANGE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                @SuppressWarnings("Anonymous2MethodRef")
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(SECURE_EXCHANGE_KEY, context, model, SecureExchangeAttribute.INSTANCE,
                            new Function<String, Object>() {
                                @Override
                                public Object apply(final String s) {
                                    return Boolean.valueOf(s);
                                }
                            });
                }
            });

    private static final SimpleAttributeDefinition SSL_CIPHER_KEY = createKey("sslCipher");
    private static final ObjectTypeAttributeDefinition SSL_CIPHER = create(
            ObjectTypeAttributeDefinition.create("ssl-cipher", SSL_CIPHER_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(SSL_CIPHER_KEY, context, model, SslCipherAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition SECURE_PROTOCOL_KEY = createKey("secureProtocol");
    private static final ObjectTypeAttributeDefinition SECURE_PROTOCOL = create(
            ObjectTypeAttributeDefinition.create("secure-protocol", SECURE_PROTOCOL_KEY),
            new ExceptionBiFunction<>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(SECURE_PROTOCOL_KEY, context, model, SecureProtocolAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition SSL_CLIENT_CERT_KEY = createKey("sslClientCert");
    private static final ObjectTypeAttributeDefinition SSL_CLIENT_CERT = create(
            ObjectTypeAttributeDefinition.create("ssl-client-cert", SSL_CLIENT_CERT_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(SSL_CLIENT_CERT_KEY, context, model, SslClientCertAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition SSL_SESSION_ID_KEY = createKey("sslSessionId");
    private static final ObjectTypeAttributeDefinition SSL_SESSION_ID = create(
            ObjectTypeAttributeDefinition.create("ssl-session-id", SSL_SESSION_ID_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(SSL_SESSION_ID_KEY, context, model, SslSessionIdAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition STORED_RESPONSE_KEY = createKey("storedResponse");
    private static final ObjectTypeAttributeDefinition STORED_RESPONSE = create(
            ObjectTypeAttributeDefinition.create("stored-response", STORED_RESPONSE_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(STORED_RESPONSE_KEY, context, model, StoredResponse.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition THREAD_NAME_KEY = createKey("threadName");
    private static final ObjectTypeAttributeDefinition THREAD_NAME = create(
            ObjectTypeAttributeDefinition.create("thread-name", THREAD_NAME_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(THREAD_NAME_KEY, context, model, ThreadNameAttribute.INSTANCE);
                }
            });

    private static final SimpleAttributeDefinition TRANSPORT_PROTOCOL_KEY = createKey("transportProtocol");
    private static final ObjectTypeAttributeDefinition TRANSPORT_PROTOCOL = create(
            ObjectTypeAttributeDefinition.create("transport-protocol", TRANSPORT_PROTOCOL_KEY),
            new ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException>() {
                @Override
                public Collection<AccessLogAttribute> apply(final OperationContext context, final ModelNode model) throws OperationFailedException {
                    return createSingleton(TRANSPORT_PROTOCOL_KEY, context, model, TransportProtocolAttribute.INSTANCE);
                }
            });

    static final ObjectTypeAttributeDefinition ATTRIBUTES = ObjectTypeAttributeDefinition.create("attributes",
            AUTHENTICATION_TYPE,
            BYTES_SENT,
            DATE_TIME,
            HOST_AND_PORT,
            LOCAL_IP,
            LOCAL_PORT,
            LOCAL_SERVER_NAME,
            PATH_PARAMETER,
            PREDICATE,
            QUERY_PARAMETER,
            QUERY_STRING,
            RELATIVE_PATH,
            REMOTE_HOST,
            REMOTE_IP,
            REMOTE_USER,
            REQUEST_HEADER,
            REQUEST_LINE,
            REQUEST_METHOD,
            REQUEST_PATH,
            REQUEST_PROTOCOL,
            REQUEST_SCHEME,
            REQUEST_URL,
            RESOLVED_PATH,
            RESPONSE_CODE,
            RESPONSE_HEADER,
            RESPONSE_REASON_PHRASE,
            RESPONSE_TIME,
            SECURE_EXCHANGE,
            SSL_CIPHER,
            SECURE_PROTOCOL,
            SSL_CLIENT_CERT,
            SSL_SESSION_ID,
            STORED_RESPONSE,
            THREAD_NAME,
            TRANSPORT_PROTOCOL
    )
            .setDefaultValue(createDefaultAttribute())
            .setRestartAllServices()
            .build();

    static Collection<AccessLogAttribute> resolveAccessLogAttribute(final AttributeDefinition attribute,
                                                                    final OperationContext context,
                                                                    final ModelNode model) throws OperationFailedException {
        final ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException> attributeResolver =
                ATTRIBUTE_RESOLVERS.get(attribute);
        assert attributeResolver != null;
        if (model.hasDefined(attribute.getName())) {
            return attributeResolver.apply(context, model.get(attribute.getName()));
        }
        return Collections.emptyList();
    }

    private static Collection<AccessLogAttribute> createSingleton(final AttributeDefinition keyAttribute,
                                                                  final OperationContext context, final ModelNode model,
                                                                  final ExchangeAttribute exchangeAttribute) throws OperationFailedException {
        return Collections.singletonList(AccessLogAttribute.of(keyAttribute.resolveModelAttribute(context, model).asString(),
                exchangeAttribute));
    }

    @SuppressWarnings({"SameParameterValue"})
    private static Collection<AccessLogAttribute> createSingleton(final AttributeDefinition keyAttribute,
                                                                  final OperationContext context, final ModelNode model,
                                                                  final ExchangeAttribute exchangeAttribute,
                                                                  final Function<String, Object> valueConverter) throws OperationFailedException {
        return Collections.singletonList(AccessLogAttribute.of(keyAttribute.resolveModelAttribute(context, model).asString(),
                exchangeAttribute, valueConverter));
    }

    private static SimpleAttributeDefinition createKey(final String dftValue) {
        return KEY_BUILDER.setDefaultValue(new ModelNode(dftValue)).build();
    }

    private static ModelNode createDefaultAttribute() {
        final ModelNode result = new ModelNode().setEmptyObject();
        result.get(REMOTE_HOST.getName()).setEmptyObject();
        result.get(REMOTE_USER.getName()).setEmptyObject();
        result.get(DATE_TIME.getName()).setEmptyObject();
        result.get(REQUEST_LINE.getName()).setEmptyObject();
        result.get(RESPONSE_CODE.getName()).setEmptyObject();
        result.get(BYTES_SENT.getName()).setEmptyObject();
        return result;
    }

    private static <R extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, R>> R create(final B builder,
                                                                                                                final ExceptionBiFunction<OperationContext, ModelNode, Collection<AccessLogAttribute>, OperationFailedException> attributeResolver) {
        final R result = builder.setRequired(false).build();
        ATTRIBUTE_RESOLVERS.put(result, attributeResolver);
        return result;
    }

    private static String resolveKeyName(final ModelNode prefix, final String name) {
        final StringBuilder result = new StringBuilder();
        if (prefix.isDefined()) {
            result.append(prefix.asString());
        }
        result.append(name);
        return result.toString();
    }
}
