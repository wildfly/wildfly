/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.math.BigDecimal;
import java.time.Duration;
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
import java.util.stream.Stream;

import io.smallrye.openapi.model.ReferenceType;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
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
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;

/**
 * Provides an OpenAPI model composed of registered models.
 * @author Paul Ferraro
 */
public class CompositeOpenAPIModelProvider implements OpenAPIModelProvider, OpenAPIModelRegistry {
    private static final Function<Optional<ExternalDocumentation>, Optional<String>> EXTERNAL_DOCUMENTATION_DESCRIPTION = optional(ExternalDocumentation::getDescription);
    private static final Function<Optional<ExternalDocumentation>, Optional<String>> EXTERNAL_DOCUMENTATION_URL = optional(ExternalDocumentation::getUrl);
    private static final Function<Optional<Contact>, Optional<String>> CONTACT_EMAIL = optional(Contact::getEmail);
    private static final Function<Optional<Contact>, Optional<String>> CONTACT_NAME = optional(Contact::getName);
    private static final Function<Optional<Contact>, Optional<String>> CONTACT_URL = optional(Contact::getUrl);
    private static final Function<Optional<License>, Optional<String>> LICENSE_IDENTIFIER = optional(License::getIdentifier);
    private static final Function<Optional<License>, Optional<String>> LICENSE_NAME = optional(License::getName);
    private static final Function<Optional<License>, Optional<String>> LICENSE_URL = optional(License::getUrl);
    private static final Function<Optional<Info>, Optional<Contact>> INFO_CONTACT = optional(Info::getContact);
    private static final Function<Optional<Info>, Optional<String>> INFO_DESCRIPTION = optional(Info::getDescription);
    private static final Function<Optional<Info>, Optional<License>> INFO_LICENSE = optional(Info::getLicense);
    private static final Function<Optional<Info>, Optional<String>> INFO_SUMMARY = optional(Info::getSummary);
    private static final Function<Optional<Info>, Optional<String>> INFO_TERMS_OF_SERVICE = optional(Info::getTermsOfService);
    private static final Function<Optional<Info>, Optional<String>> INFO_TITLE = optional(Info::getTitle);
    private static final Function<Optional<Info>, Optional<String>> INFO_VERSION = optional(Info::getVersion);
    private static final Function<Optional<OpenAPI>, Optional<Components>> COMPONENTS = optional(OpenAPI::getComponents);
    private static final Function<Optional<OpenAPI>, Optional<ExternalDocumentation>> EXTERNAL_DOCUMENTATION = optional(OpenAPI::getExternalDocs);
    private static final Function<Optional<OpenAPI>, Optional<Info>> INFO = optional(OpenAPI::getInfo);
    private static final Function<Optional<OpenAPI>, Optional<Contact>> CONTACT = INFO.andThen(INFO_CONTACT);
    private static final Function<Optional<OpenAPI>, Optional<String>> JSON_SCHEMA_DIALECT = optional(OpenAPI::getJsonSchemaDialect);
    private static final Function<Optional<OpenAPI>, Optional<License>> LICENSE = INFO.andThen(INFO_LICENSE);
    private static final Function<Optional<OpenAPI>, Optional<Paths>> PATHS = optional(OpenAPI::getPaths);
    private static final Function<Optional<OpenAPI>, Optional<List<Server>>> SERVERS = optional(OpenAPI::getServers);
    private static final Function<Optional<OpenAPI>, Optional<String>> VERSION = optional(OpenAPI::getOpenapi);

    // N.B. Neither the MP OpenAPI spec nor SmallRye provides a means to distinguish between a property of a standard vs a custom schema
    // The intention of the madness below is to collect the property names used by a standard schema
    // Anything property not defined by this set must therefore belong to a custom schema
    private static final Set<String> STANDARD_SCHEMA_PROPERTIES = new Supplier<Schema>() {
        @Override
        public Schema get() {
            Schema schema = OASFactory.createSchema();
            // Populate all standard fields so that we can determine their property names.
            return OASFactory.createSchema()
                    .additionalPropertiesSchema(schema)
                    .allOf(List.of(schema))
                    .anyOf(List.of(schema))
                    .comment("")
                    .constValue(Duration.ZERO)
                    .contains(schema)
                    .contentEncoding("")
                    .contentMediaType("")
                    .contentSchema(schema)
                    .defaultValue("")
                    .dependentRequired(Map.of("", List.of("")))
                    .dependentSchemas(Map.of("", schema))
                    .deprecated(Boolean.FALSE)
                    .description("")
                    .discriminator(OASFactory.createDiscriminator())
                    .elseSchema(schema)
                    .enumeration(List.of(""))
                    .examples(List.of(""))
                    .exclusiveMaximum(BigDecimal.ZERO)
                    .exclusiveMinimum(BigDecimal.ZERO)
                    .extensions(Map.of("", Duration.ZERO))
                    .externalDocs(OASFactory.createExternalDocumentation())
                    .format("")
                    .ifSchema(schema)
                    .items(schema)
                    .maxContains(Integer.valueOf(0))
                    .maximum(BigDecimal.ZERO)
                    .maxItems(Integer.valueOf(0))
                    .maxLength(Integer.valueOf(0))
                    .maxProperties(Integer.valueOf(0))
                    .minContains(Integer.valueOf(0))
                    .minimum(BigDecimal.ZERO)
                    .minItems(Integer.valueOf(0))
                    .minLength(Integer.valueOf(0))
                    .minProperties(Integer.valueOf(0))
                    .multipleOf(BigDecimal.ZERO)
                    .not(schema)
                    .oneOf(List.of(schema))
                    .pattern("")
                    .patternProperties(Map.of("", schema))
                    .prefixItems(List.of(schema))
                    .properties(Map.of("", schema))
                    .propertyNames(schema)
                    .readOnly(Boolean.TRUE)
                    .ref("")
                    .required(List.of(""))
                    .schemaDialect("")
                    .thenSchema(schema)
                    .title("")
                    .type(List.of(Schema.SchemaType.NULL))
                    .unevaluatedItems(schema)
                    .unevaluatedProperties(schema)
                    .uniqueItems(Boolean.FALSE)
                    .writeOnly(Boolean.FALSE)
                    .xml(OASFactory.createXML())
                    ;
        }
    }.get().getAll().keySet();

    private static <T, R> Function<Optional<T>, Optional<R>> optional(Function<T, R> function) {
        return new Function<>() {
            @Override
            public Optional<R> apply(Optional<T> value) {
                return value.map(function);
            }
        };
    }

    private final Optional<OpenAPI> defaultModel;
    private final BinaryOperator<String> resolver;
    private final StampedLock lock = new StampedLock();
    private final Map<String, Optional<OpenAPI>> models = new TreeMap<>();
    // We will read/write cached model using plain references guarded by lock
    private final AtomicReference<OpenAPI> provider = new AtomicReference<>();

    CompositeOpenAPIModelProvider(OpenAPI defaultModel, BinaryOperator<String> resolver) {
        this.defaultModel = Optional.of(defaultModel);
        this.resolver = resolver;
    }

    @Override
    public Registration register(String key, OpenAPI model) {
        Optional<OpenAPI> optionalModel = Optional.of(model);
        long stamp = this.lock.writeLock();
        try {
            if (this.models.put(key, optionalModel) != optionalModel) {
                // Invalidate cached model
                this.provider.setPlain(null);
            }
            return () -> this.remove(key);
        } finally {
            this.lock.unlock(stamp);
        }
    }

    void remove(String key) {
        long stamp = this.lock.writeLock();
        try {
            if (this.models.remove(key) != null) {
                // Invalidate cached model
                this.provider.setPlain(null);
            }
        } finally {
            this.lock.unlock(stamp);
        }
    }

    @Override
    public Optional<OpenAPI> getDefaultModel() {
        return this.defaultModel;
    }

    @Override
    public Map<String, Optional<OpenAPI>> getModels() {
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
                    // Generate model with write lock
                    model = new OpenAPIModelFactory(this.models).getModel();
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

    private class OpenAPIModelFactory implements OpenAPIModelProvider {
        private final Map<String, Optional<OpenAPI>> models;

        OpenAPIModelFactory(Map<String, Optional<OpenAPI>> models) {
            this.models = models;
        }

        @Override
        public OpenAPI getModel() {
            OpenAPI result = OASFactory.createOpenAPI()
                    .components(OASFactory.createComponents().extensions(this.extensions(COMPONENTS)))
                    .extensions(this.extensions(UnaryOperator.identity()))
                    .externalDocs(OASFactory.createExternalDocumentation()
                            .description(this.singleton(EXTERNAL_DOCUMENTATION.andThen(EXTERNAL_DOCUMENTATION_DESCRIPTION)))
                            .extensions(this.extensions(EXTERNAL_DOCUMENTATION))
                            .url(this.singleton(EXTERNAL_DOCUMENTATION.andThen(EXTERNAL_DOCUMENTATION_URL))))
                    .info(OASFactory.createInfo()
                            .contact(OASFactory.createContact()
                                    .email(this.singleton(CONTACT.andThen(CONTACT_EMAIL)))
                                    .extensions(this.extensions(CONTACT))
                                    .name(this.singleton(CONTACT.andThen(CONTACT_NAME)))
                                    .url(this.singleton(CONTACT.andThen(CONTACT_URL))))
                            .description(this.singleton(INFO.andThen(INFO_DESCRIPTION)))
                            .extensions(this.extensions(INFO))
                            .license(OASFactory.createLicense()
                                    .extensions(this.extensions(LICENSE))
                                    .identifier(this.singleton(LICENSE.andThen(LICENSE_IDENTIFIER)))
                                    .name(this.singleton(LICENSE.andThen(LICENSE_NAME)))
                                    .url(this.singleton(LICENSE.andThen(LICENSE_URL))))
                            .summary(this.singleton(INFO.andThen(INFO_SUMMARY)))
                            .termsOfService(this.singleton(INFO.andThen(INFO_TERMS_OF_SERVICE)))
                            .title(this.singleton(INFO.andThen(INFO_TITLE)))
                            .version(this.singleton(INFO.andThen(INFO_VERSION))))
                    .jsonSchemaDialect(this.singleton(JSON_SCHEMA_DIALECT))
                    .openapi(this.singleton(VERSION))
                    .paths(OASFactory.createPaths().extensions(this.extensions(PATHS)))
                    .servers(this.list(SERVERS))
                    ;

            // For named components, resolve distinct names if necessary to avoid collisions
            for (Map.Entry<String, Optional<OpenAPI>> entry : this.models.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue().isPresent()) {
                    OpenAPI model = entry.getValue().get();
                    // If there are multiple deployments, resolve distinct component names using deployment name
                    UnaryOperator<String> resolver = (this.models.size() > 1) ? name -> CompositeOpenAPIModelProvider.this.resolver.apply(key, name) : UnaryOperator.identity();

                    Components components = model.getComponents();
                    if (components != null) {
                        addCallbacks(components.getCallbacks(), resolve(result.getComponents()::addCallback, resolver), resolver);
                        addExamples(components.getExamples(), resolve(result.getComponents()::addExample, resolver), resolver);
                        addHeaders(components.getHeaders(), resolve(result.getComponents()::addHeader, resolver), resolver);
                        addLinks(components.getLinks(), resolve(result.getComponents()::addLink, resolver), resolver);
                        addParameters(components.getParameters(), resolve(result.getComponents()::addParameter, resolver), resolver);
                        addPathItems(components.getPathItems(), resolve(result.getComponents()::addPathItem, resolver), resolver);
                        addRequestBodies(components.getRequestBodies(), resolve(result.getComponents()::addRequestBody, resolver), resolver);
                        addResponses(components.getResponses(), resolve(result.getComponents()::addResponse, resolver), resolver);
                        addSchemas(components.getSchemas(), resolve(result.getComponents()::addSchema, resolver), resolver);
                        addSecuritySchemes(components.getSecuritySchemes(), resolve(result.getComponents()::addSecurityScheme, resolver), resolver);
                    }

                    Paths paths = model.getPaths();
                    if (paths != null) {
                        addPathItems(paths.getPathItems(), result.getPaths()::addPathItem, resolver);
                    }

                    addSecurityRequirements(model.getSecurity(), result::addSecurityRequirement, resolver);
                    addTags(model.getTags(), result::addTag, resolver);
                    addPathItems(model.getWebhooks(), resolve(result::addWebhook, resolver), resolver);
                }
            }

            return result;
        }

        private String singleton(Function<Optional<OpenAPI>, Optional<String>> function) {
            // For singleton properties, use value from host model if present, otherwise use concordant value from deployment models
            return function.apply(CompositeOpenAPIModelProvider.this.defaultModel).orElseGet(() -> this.models.values().stream().map(function).filter(Optional::isPresent).map(Optional::get).distinct().collect(new SingletonCollector<>()).orElse(null));
        }

        private <T extends Extensible<T>> Map<String, Object> extensions(Function<Optional<OpenAPI>, Optional<T>> component) {
            Stream<T> defaultComponents = component.apply(CompositeOpenAPIModelProvider.this.defaultModel).map(Stream::of).orElse(Stream.empty());
            Stream<T> deploymentComponents = this.models.values().stream().map(component).filter(Optional::isPresent).map(Optional::get);
            return Stream.concat(defaultComponents, deploymentComponents).map(Extensible::getExtensions).filter(Objects::nonNull).map(Map::entrySet).flatMap(Set::stream)
                    // Drop conflicts
                    .distinct().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> null, TreeMap::new));
        }

        private <T> List<T> list(Function<Optional<OpenAPI>, Optional<List<T>>> components) {
            Stream<T> defaultComponents = components.apply(CompositeOpenAPIModelProvider.this.defaultModel).orElse(List.of()).stream();
            Stream<T> deploymentComponents = this.models.values().stream().map(components).filter(Optional::isPresent).map(Optional::get).flatMap(List::stream);
            return Stream.concat(defaultComponents, deploymentComponents).distinct().toList();
        }
    }

    private static <T> BiConsumer<String, T> resolve(BiConsumer<String, T> accumulator, UnaryOperator<String> resolver) {
        return new BiConsumer<>() {
            @Override
            public void accept(String name, T value) {
                accumulator.accept(resolver.apply(name), value);
            }
        };
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

    private static void addTags(List<Tag> tags, Consumer<Tag> accumulator, UnaryOperator<String> resolver) {
        if (tags != null) {
            for (Tag tag : tags) {
                accumulator.accept(resolve(tag, resolver));
            }
        }
    }

    private static void addTagRefs(List<String> tags, Consumer<String> accumulator, UnaryOperator<String> resolver) {
        if (tags != null) {
            for (String tag : tags) {
                accumulator.accept(resolver.apply(tag));
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
        return resolveReference(ReferenceType.LINK, Link.class, link, resolver)
                .description(link.getDescription())
                .operationId(link.getOperationId())
                .operationRef(resolve(ReferenceType.PATH_ITEM, link.getOperationRef(), resolver))
                .requestBody(link.getRequestBody())
                .parameters(link.getParameters())
                .server(link.getServer())
                ;
    }

    private static MediaType resolve(MediaType mediaType, UnaryOperator<String> resolver) {
        MediaType result = (mediaType != null) ? resolveExtensible(MediaType.class, mediaType, resolver)
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
        Operation result = resolveExtensible(Operation.class, operation, resolver)
                .deprecated(operation.getDeprecated())
                .description(operation.getDescription())
                .externalDocs(operation.getExternalDocs())
                .operationId(operation.getOperationId())
                .requestBody(resolve(operation.getRequestBody(), resolver))
                .responses(resolve(operation.getResponses(), resolver))
                .servers(operation.getServers())
                .summary(operation.getSummary())
                ;
        addCallbacks(operation.getCallbacks(), result::addCallback, resolver);
        addParameters(operation.getParameters(), result::addParameter, resolver);
        addSecurityRequirements(operation.getSecurity(), result::addSecurityRequirement, resolver);
        addTagRefs(operation.getTags(), result::addTag, resolver);
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
        APIResponses result = (responses != null) ? resolveExtensible(APIResponses.class, responses, resolver)
                .defaultValue(resolve(responses.getDefaultValue(), resolver))
                : null;
        if (result != null) {
            addResponses(responses.getAPIResponses(), result::addAPIResponse, resolver);
        }
        return result;
    }

    private static Schema resolve(Schema schema, UnaryOperator<String> resolver) {
        // N.B. For some reason, a boolean schema does not have its own interface and instead requires special treatment
        Schema result = (schema != null) ? resolveReference(ReferenceType.SCHEMA, Schema.class, schema, resolver).booleanSchema(schema.getBooleanSchema()) : null;
        if (result != null) {
            if (result.getBooleanSchema() == null) {
                result.additionalPropertiesSchema(resolve(schema.getAdditionalPropertiesSchema(), resolver))
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
                    .type(schema.getType())
                    .unevaluatedItems(resolve(schema.getUnevaluatedItems(), resolver))
                    .unevaluatedProperties(resolve(schema.getUnevaluatedProperties(), resolver))
                    .uniqueItems(schema.getUniqueItems())
                    .writeOnly(schema.getWriteOnly())
                    .xml(schema.getXml())
                    ;
                // Schema API provides no mechanism to distinguish custom properties from standard properties.
                for (Map.Entry<String, ?> entry : schema.getAll().entrySet()) {
                    if (!STANDARD_SCHEMA_PROPERTIES.contains(entry.getKey())) {
                        result.set(entry.getKey(), entry.getValue());
                    }
                }
            }
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

    private static Tag resolve(Tag tag, UnaryOperator<String> resolver) {
        return resolveExtensible(Tag.class, tag, resolver)
                .description(tag.getDescription())
                .externalDocs(tag.getExternalDocs())
                .name(resolver.apply(tag.getName()))
                ;
    }

    private static <T extends Reference<T> & Extensible<T> & Constructible> T resolveReference(ReferenceType type, Class<T> componentClass, T component, UnaryOperator<String> resolver) {
        return (component != null) ? resolveExtensible(componentClass, component, resolver).ref(resolve(type, component.getRef(), resolver)) : null;
    }

    private static <T extends Extensible<T> & Constructible> T resolveExtensible(Class<T> componentClass, T component, UnaryOperator<String> resolver) {
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
