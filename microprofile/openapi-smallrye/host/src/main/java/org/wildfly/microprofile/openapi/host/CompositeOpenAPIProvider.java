/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.smallrye.openapi.model.ReferenceType;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;

/**
 * Provides an OpenAPI model composed of registered models.
 * @author Paul Ferraro
 */
public class CompositeOpenAPIProvider implements OpenAPIModelProvider, OpenAPIModelRegistry {

    private final OpenAPI defaultModel;
    private final BinaryOperator<String> resolver;
    private final StampedLock lock = new StampedLock();
    private final Map<String, OpenAPI> models = new TreeMap<>();
    private final AtomicReference<OpenAPI> provider = new AtomicReference<>();

    CompositeOpenAPIProvider(OpenAPI defaultModel, BinaryOperator<String> resolver) {
        this.defaultModel = defaultModel;
        this.resolver = resolver;
    }

    @Override
    public Registration register(String key, OpenAPI model) {
        long stamp = this.lock.writeLock();
        try {
            if (this.models.put(key, model) != model) {
                this.provider.setPlain(null);
            }
            return new Registration() {
                @Override
                public void close() {
                    long stamp = CompositeOpenAPIProvider.this.lock.writeLock();
                    try {
                        if (CompositeOpenAPIProvider.this.models.remove(key) != null) {
                            CompositeOpenAPIProvider.this.provider.setPlain(null);
                        }
                    } finally {
                        CompositeOpenAPIProvider.this.lock.unlock(stamp);
                    }
                }
            };
        } finally {
            this.lock.unlock(stamp);
        }
    }

    @Override
    public Map<String, OpenAPI> getModels() {
        long stamp = this.lock.readLock();
        try {
            return Map.copyOf(this.models);
        } finally {
            this.lock.unlock(stamp);
        }
    }

    @Override
    public OpenAPI getModel() {
        // Logic copied from org.wildfly.clustering.server.util.BlockingReference.ConditionalReferenceWriter
        OpenAPI model = null;
        boolean update = false;
        // Try optimistic read first
        long stamp = this.lock.tryOptimisticRead();
        try {
            if (StampedLock.isOptimisticReadStamp(stamp)) {
                // Read optimistically, and validate later
                model = this.provider.getPlain();
                update = (model == null);
            }
            if (!this.lock.validate(stamp)) {
                // Optimistic read unsuccessful or invalid
                // Acquire pessimistic read lock
                stamp = this.lock.readLock();
                // Re-read with read lock
                model = this.provider.getPlain();
                update = (model == null);
            }
            if (update) {
                long conversionStamp = this.lock.tryConvertToWriteLock(stamp);
                if (StampedLock.isWriteLockStamp(conversionStamp)) {
                    // Read -> write lock upgrade successful
                    stamp = conversionStamp;
                } else {
                    // Lock upgrade unsuccessful, release any pessimistic read lock and acquire write lock
                    if (StampedLock.isReadLockStamp(stamp)) {
                        this.lock.unlockRead(stamp);
                    }
                    stamp = this.lock.writeLock();
                    // Re-read with write lock
                    model = this.provider.getPlain();
                    update = (model == null);
                }
                if (update) {
                    model = this.createModel();
                    this.provider.setPlain(model);
                }
            }
            return model;
        } finally {
            if (StampedLock.isLockStamp(stamp)) {
                this.lock.unlock(stamp);
            }
        }
    }

    // Invoked with write lock
    private OpenAPI createModel() {
        // Single deployment use case
        if (this.models.size() == 1) return this.models.values().iterator().next();

        // If there are OpenAPI models for multiple deployments, merge into single model
        OpenAPI result = OASFactory.createOpenAPI()
                .externalDocs(this.distinct(this.models.values(), OpenAPI::getExternalDocs))
                .info(this.distinct(this.models.values(), OpenAPI::getInfo))
                .jsonSchemaDialect(this.distinct(this.models.values(), OpenAPI::getJsonSchemaDialect))
                .openapi(this.distinct(this.models.values(), OpenAPI::getOpenapi))
                ;

        Components resultComponents = OASFactory.createComponents();
        Paths resultPaths = OASFactory.createPaths();
        for (Map.Entry<String, OpenAPI> entry : this.models.entrySet()) {
            String key = entry.getKey();
            OpenAPI model = entry.getValue();
            // Resolve distinct component names using deployment name
            UnaryOperator<String> resolver = name -> this.resolver.apply(key, name);

            addAll(model.getExtensions(), result::addExtension);

            Components components = model.getComponents();
            if (components != null) {
                addAll(components.getExtensions(), resultComponents::addExtension);
                addCallbacks(components.getCallbacks(), resolve(resultComponents::addCallback, resolver), resolver);
                addExamples(components.getExamples(), resolve(resultComponents::addExample, resolver), resolver);
                addHeaders(components.getHeaders(), resolve(resultComponents::addHeader, resolver), resolver);
                addLinks(components.getLinks(), resolve(resultComponents::addLink, resolver), resolver);
                addParameters(components.getParameters(), resolve(resultComponents::addParameter, resolver), resolver);
                addPathItems(components.getPathItems(), resolve(resultComponents::addPathItem, resolver), resolver);
                addRequestBodies(components.getRequestBodies(), resolve(resultComponents::addRequestBody, resolver), resolver);
                addResponses(components.getResponses(), resolve(resultComponents::addResponse, resolver), resolver);
                addSchemas(components.getSchemas(), resolve(resultComponents::addSchema, resolver), resolver);
                addSecuritySchemes(components.getSecuritySchemes(), resolve(resultComponents::addSecurityScheme, resolver), resolver);
            }

            Paths paths = model.getPaths();
            if (paths != null) {
                addAll(paths.getExtensions(), resultPaths::addExtension);
                // N.B. Paths will already be unique per deployment model
                addPathItems(paths.getPathItems(), resultPaths::addPathItem, resolver);
            }

            addSecurityRequirements(model.getSecurity(), result::addSecurityRequirement, resolver);
            addPathItems(model.getWebhooks(), resolve(result::addWebhook, resolver), resolver);
        }

        result.setComponents(resultComponents);
        result.setPaths(resultPaths);

        addDistinct(this.models.values(), OpenAPI::getServers, result::addServer);
        addDistinct(this.models.values(), OpenAPI::getTags, result::addTag);

        return result;
    }

    private static <T> BiConsumer<String, T> resolve(BiConsumer<String, T> accumulator, UnaryOperator<String> resolver) {
        return new BiConsumer<>() {
            @Override
            public void accept(String name, T value) {
                accumulator.accept(resolver.apply(name), value);
            }
        };
    }

    private <T> T distinct(Collection<OpenAPI> models, Function<OpenAPI, T> function) {
        // If registered models contain conflicting values use value from default model
        return models.stream().map(function).filter(Objects::nonNull).distinct().collect(new SingletonCollector<>()).orElse(function.apply(this.defaultModel));
    }

    private <T> void addDistinct(Collection<OpenAPI> models, Function<OpenAPI, List<T>> function, Consumer<T> accumulator) {
        List<T> values = models.stream().map(function).filter(Objects::nonNull).flatMap(List::stream).distinct().toList();
        (values.isEmpty() ? Optional.ofNullable(function.apply(this.defaultModel)).orElse(List.of()) : values).forEach(accumulator);
    }

    private static <T> void addAll(List<T> list, Consumer<T> consumer) {
        if (list != null) {
            list.forEach(consumer);
        }
    }

    private static <T> void addAll(Map<String, T> map, BiConsumer<String, T> consumer) {
        if (map != null) {
            map.forEach(consumer);
        }
    }

    private static void addCallbacks(Map<String, Callback> callbacks, BiConsumer<String, Callback> accumulator, UnaryOperator<String> resolver) {
        if (callbacks != null) {
            for (Map.Entry<String, Callback> entry : callbacks.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addExamples(Map<String, Example> examples, BiConsumer<String, Example> accumulator, UnaryOperator<String> resolver) {
        if (examples != null) {
            for (Map.Entry<String, Example> entry : examples.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addHeaders(Map<String, Header> headers, BiConsumer<String, Header> accumulator, UnaryOperator<String> resolver) {
        if (headers != null) {
            for (Map.Entry<String, Header> entry : headers.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addLinks(Map<String, Link> links, BiConsumer<String, Link> accumulator, UnaryOperator<String> resolver) {
        if (links != null) {
            for (Map.Entry<String, Link> entry : links.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addMediaTypes(Map<String, MediaType> mediaTypes, BiConsumer<String, MediaType> accumulator, UnaryOperator<String> resolver) {
        if (mediaTypes != null) {
            for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addOperations(Map<HttpMethod, Operation> operations, BiConsumer<HttpMethod, Operation> accumulator, UnaryOperator<String> resolver) {
        if (operations != null) {
            for (Map.Entry<HttpMethod, Operation> entry : operations.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addParameters(Map<String, Parameter> parameters, BiConsumer<String, Parameter> accumulator, UnaryOperator<String> resolver) {
        if (parameters != null) {
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addParameters(List<Parameter> parameters, Consumer<Parameter> accumulator, UnaryOperator<String> resolver) {
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                accumulator.accept(resolve(parameter, resolver));
            }
        }
    }

    private static void addPathItems(Map<String, PathItem> pathItems, BiConsumer<String, PathItem> accumulator, UnaryOperator<String> resolver) {
        if (pathItems != null) {
            for (Map.Entry<String, PathItem> entry : pathItems.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addRequestBodies(Map<String, RequestBody> requestBodies, BiConsumer<String, RequestBody> accumulator, UnaryOperator<String> resolver) {
        if (requestBodies != null) {
            for (Map.Entry<String, RequestBody> entry : requestBodies.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addResponses(Map<String, APIResponse> responses, BiConsumer<String, APIResponse> accumulator, UnaryOperator<String> resolver) {
        if (responses != null) {
            for (Map.Entry<String, APIResponse> entry : responses.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addSchemas(List<Schema> schemas, Consumer<Schema> accumulator, UnaryOperator<String> resolver) {
        if (schemas != null) {
            for (Schema schema : schemas) {
                accumulator.accept(resolve(schema, resolver));
            }
        }
    }

    private static void addSchemas(Map<String, Schema> schemas, BiConsumer<String, Schema> accumulator, UnaryOperator<String> resolver) {
        if (schemas != null) {
            for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static void addSecurityRequirements(List<SecurityRequirement> requirements, Consumer<SecurityRequirement> accumulator, UnaryOperator<String> resolver) {
        if (requirements != null) {
            for (SecurityRequirement requirement : requirements) {
                accumulator.accept(resolve(requirement, resolver));
            }
        }
    }

    private static void addSecuritySchemes(Map<String, SecurityScheme> schemes, BiConsumer<String, SecurityScheme> accumulator, UnaryOperator<String> resolver) {
        if (schemes != null) {
            for (Map.Entry<String, SecurityScheme> entry : schemes.entrySet()) {
                accumulator.accept(entry.getKey(), resolve(entry.getValue(), resolver));
            }
        }
    }

    private static Callback resolve(Callback callback, UnaryOperator<String> resolver) {
        Callback result = resolveReference(ReferenceType.CALLBACK, Callback.class, callback, resolver);
        addPathItems(callback.getPathItems(), result::addPathItem, resolver);
        return result;
    }

    private static Content resolve(Content content, UnaryOperator<String> resolver) {
        Content result = (content != null) ? OASFactory.createContent() : null;
        if (result != null) {
            addMediaTypes(content.getMediaTypes(), result::addMediaType, resolver);
        }
        return result;
    }

    private static Example resolve(Example example, UnaryOperator<String> resolver) {
        return resolveReference(ReferenceType.EXAMPLE, Example.class, example, resolver)
                .description(example.getDescription())
                .externalValue(resolve(ReferenceType.EXAMPLE, example.getExternalValue(), resolver))
                .summary(example.getSummary())
                .value(example.getValue())
                ;
    }

    private static Header resolve(Header header, UnaryOperator<String> resolver) {
        Header result = resolveReference(ReferenceType.HEADER, Header.class, header, resolver)
                .allowEmptyValue(header.getAllowEmptyValue())
                .content(resolve(header.getContent(), resolver))
                .deprecated(header.getDeprecated())
                .description(header.getDescription())
                .example(header.getExample())
                .explode(header.getExplode())
                .required(header.getRequired())
                .schema(resolve(header.getSchema(), resolver))
                .style(header.getStyle())
                ;
        addExamples(header.getExamples(), result::addExample, resolver);
        return result;
    }

    private static Link resolve(Link link, UnaryOperator<String> resolver) {
        Link result = resolveReference(ReferenceType.LINK, Link.class, link, resolver)
                .description(link.getDescription())
                .operationId(link.getOperationId())
                .operationRef(resolve(ReferenceType.PATH_ITEM, link.getOperationRef(), resolver))
                .requestBody(link.getRequestBody())
                .parameters(link.getParameters())
                .server(link.getServer())
                ;
        return result;
    }

    private static MediaType resolve(MediaType mediaType, UnaryOperator<String> resolver) {
        MediaType result = (mediaType != null) ? resolveExtensibleReference(MediaType.class, mediaType, resolver)
                .encoding(mediaType.getEncoding())
                .example(mediaType.getExample())
                .extensions(mediaType.getExtensions())
                .schema(resolve(mediaType.getSchema(), resolver))
                : null;
        if (result != null) {
            addExamples(mediaType.getExamples(), result::addExample, resolver);
        }
        return result;
    }

    private static Operation resolve(Operation operation, UnaryOperator<String> resolver) {
        Operation result = resolveExtensibleReference(Operation.class, operation, resolver)
                .deprecated(operation.getDeprecated())
                .description(operation.getDescription())
                .extensions(operation.getExtensions())
                .externalDocs(operation.getExternalDocs())
                .operationId(operation.getOperationId())
                .requestBody(resolve(operation.getRequestBody(), resolver))
                .responses(resolve(operation.getResponses(), resolver))
                .servers(operation.getServers())
                .summary(operation.getSummary())
                .tags(operation.getTags())
                ;
        addCallbacks(operation.getCallbacks(), result::addCallback, resolver);
        addParameters(operation.getParameters(), result::addParameter, resolver);
        addSecurityRequirements(operation.getSecurity(), result::addSecurityRequirement, resolver);
        return result;
    }

    private static Parameter resolve(Parameter parameter, UnaryOperator<String> resolver) {
        Parameter result = (parameter != null) ? resolveReference(ReferenceType.PARAMETER, Parameter.class, parameter, resolver)
                .allowEmptyValue(parameter.getAllowEmptyValue())
                .allowReserved(parameter.getAllowReserved())
                .content(resolve(parameter.getContent(), resolver))
                .deprecated(parameter.getDeprecated())
                .description(parameter.getDescription())
                .example(parameter.getExample())
                .explode(parameter.getExplode())
                .in(parameter.getIn())
                .name(parameter.getName())
                .required(parameter.getRequired())
                .schema(resolve(parameter.getSchema(), resolver))
                .style(parameter.getStyle())
                : null;
        if (result != null) {
            addExamples(parameter.getExamples(), result::addExample, resolver);
        }
        return result;
    }

    private static PathItem resolve(PathItem pathItem, UnaryOperator<String> resolver) {
        PathItem result = resolveReference(ReferenceType.PATH_ITEM, PathItem.class, pathItem, resolver)
                .description(pathItem.getDescription())
                .servers(pathItem.getServers())
                .summary(pathItem.getSummary())
                ;
        addOperations(pathItem.getOperations(), result::setOperation, resolver);
        addParameters(pathItem.getParameters(), result::addParameter, resolver);
        return result;
    }

    private static RequestBody resolve(RequestBody requestBody, UnaryOperator<String> resolver) {
        return (requestBody != null) ? resolveReference(ReferenceType.REQUEST_BODY, RequestBody.class, requestBody, resolver)
                .content(resolve(requestBody.getContent(), resolver))
                .description(requestBody.getDescription())
                .required(requestBody.getRequired())
                : null;
    }

    private static APIResponse resolve(APIResponse response, UnaryOperator<String> resolver) {
        APIResponse result = (response != null) ? resolveReference(ReferenceType.RESPONSE, APIResponse.class, response, resolver)
                .content(resolve(response.getContent(), resolver))
                .description(response.getDescription())
                : null;
        if (result != null) {
            addHeaders(response.getHeaders(), result::addHeader, resolver);
            addLinks(response.getLinks(), result::addLink, resolver);
        }
        return result;
    }

    private static APIResponses resolve(APIResponses responses, UnaryOperator<String> resolver) {
        APIResponses result = (responses != null) ? resolveExtensibleReference(APIResponses.class, responses, resolver)
                .defaultValue(resolve(responses.getDefaultValue(), resolver)).extensions(null)
                : null;
        if (result != null) {
            addResponses(responses.getAPIResponses(), result::addAPIResponse, resolver);
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static Schema resolve(Schema schema, UnaryOperator<String> resolver) {
        Schema result = (schema != null) ? resolveReference(ReferenceType.SCHEMA, Schema.class, schema, resolver)
                .additionalPropertiesBoolean(schema.getAdditionalPropertiesBoolean())
                .additionalPropertiesSchema(resolve(schema.getAdditionalPropertiesSchema(), resolver))
                .booleanSchema(schema.getBooleanSchema())
                .comment(schema.getComment())
                .constValue(schema.getConstValue())
                .contains(resolve(schema.getContains(), resolver))
                .contentEncoding(schema.getContentEncoding())
                .contentMediaType(schema.getContentMediaType())
                .contentSchema(resolve(schema.getContentSchema(), resolver))
                .defaultValue(schema.getDefaultValue())
                .dependentRequired(schema.getDependentRequired())
                .deprecated(schema.getDeprecated())
                .description(schema.getDescription())
                .discriminator(schema.getDiscriminator())
                .elseSchema(resolve(schema.getElseSchema(), resolver))
                .enumeration(schema.getEnumeration())
                .example(schema.getExample())
                .examples(schema.getExamples())
                .exclusiveMaximum(schema.getExclusiveMaximum())
                .exclusiveMinimum(schema.getExclusiveMinimum())
                .externalDocs(schema.getExternalDocs())
                .format(schema.getFormat())
                .ifSchema(resolve(schema.getIfSchema(), resolver))
                .items(resolve(schema.getItems(), resolver))
                .maxContains(schema.getMaxContains())
                .maximum(schema.getMaximum())
                .maxItems(schema.getMaxItems())
                .maxLength(schema.getMaxLength())
                .maxProperties(schema.getMaxProperties())
                .minContains(schema.getMinContains())
                .minimum(schema.getMinimum())
                .minItems(schema.getMinItems())
                .minLength(schema.getMinLength())
                .minProperties(schema.getMinProperties())
                .multipleOf(schema.getMultipleOf())
                .not(resolve(schema.getNot(), resolver))
                .pattern(schema.getPattern())
                .propertyNames(resolve(schema.getPropertyNames(), resolver))
                .readOnly(schema.getReadOnly())
                .required(schema.getRequired())
                .schemaDialect(schema.getSchemaDialect())
                .thenSchema(resolve(schema.getThenSchema(), resolver))
                .title(schema.getTitle())
                .unevaluatedItems(resolve(schema.getUnevaluatedItems(), resolver))
                .unevaluatedProperties(resolve(schema.getUnevaluatedProperties(), resolver))
                .uniqueItems(schema.getUniqueItems())
                .writeOnly(schema.getWriteOnly())
                .xml(schema.getXml())
                : null;
        if (result != null) {
            addSchemas(schema.getAllOf(), result::addAllOf, resolver);
            addSchemas(schema.getAnyOf(), result::addAnyOf, resolver);
            addSchemas(schema.getDependentSchemas(), result::addDependentSchema, resolver);
            addSchemas(schema.getOneOf(), result::addOneOf, resolver);
            addSchemas(schema.getPatternProperties(), result::addPatternProperty, resolver);
            addSchemas(schema.getPrefixItems(), result::addPrefixItem, resolver);
            addSchemas(schema.getProperties(), result::addProperty, resolver);
        }
        return result;
    }

    private static SecurityRequirement resolve(SecurityRequirement requirement, UnaryOperator<String> resolver) {
        SecurityRequirement result = OASFactory.createSecurityRequirement();
        for (Map.Entry<String, List<String>> entry : Optional.ofNullable(requirement.getSchemes()).orElse(Map.of()).entrySet()) {
            result.addScheme(resolver.apply(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static SecurityScheme resolve(SecurityScheme scheme, UnaryOperator<String> resolver) {
        return resolveReference(ReferenceType.SECURITY_SCHEME, SecurityScheme.class, scheme, resolver)
                .bearerFormat(scheme.getBearerFormat())
                .description(scheme.getDescription())
                .flows(scheme.getFlows())
                .in(scheme.getIn())
                .name(scheme.getName())
                .openIdConnectUrl(scheme.getOpenIdConnectUrl())
                .scheme(scheme.getScheme())
                .type(scheme.getType())
                ;
    }

    private static <T extends Reference<T> & Extensible<T> & Constructible> T resolveReference(ReferenceType type, Class<T> componentClass, T component, UnaryOperator<String> resolver) {
        return (component != null) ? resolveExtensibleReference(componentClass, component, resolver).ref(resolve(type, component.getRef(), resolver)) : null;
    }

    private static <T extends Extensible<T> & Constructible> T resolveExtensibleReference(Class<T> componentClass, T component, UnaryOperator<String> resolver) {
        return (component != null) ? OASFactory.createObject(componentClass).extensions(component.getExtensions()) : null;
    }

    private static <T extends Reference<T> & Constructible> String resolve(ReferenceType type, String value, UnaryOperator<String> resolver) {
        return ((value != null) && value.startsWith(type.referencePrefix())) ? type.referenceOf(resolver.apply(value.substring(type.referencePrefix().length() + 1))) : value;
    }

    private static class SingletonCollector<T> implements Collector<T, Object, Optional<T>>, Function<List<T>, Optional<T>> {
        private final Collector<T, Object, Optional<T>> collector;

        SingletonCollector() {
            this.collector = Collectors.collectingAndThen(Collectors.toList(), this);
        }

        @Override
        public Optional<T> apply(List<T> list) {
            return (list.size() == 1) ? Optional.of(list.get(0)) : Optional.empty();
        }

        @Override
        public Supplier<Object> supplier() {
            return this.collector.supplier();
        }

        @Override
        public BiConsumer<Object, T> accumulator() {
            return this.collector.accumulator();
        }

        @Override
        public BinaryOperator<Object> combiner() {
            return this.collector.combiner();
        }

        @Override
        public Function<Object, Optional<T>> finisher() {
            return this.collector.finisher();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return this.collector.characteristics();
        }
    }
}
