package org.wildfly.extension.undertow.filters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.undertow.deployment.ConfiguredHandlerWrapper;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class CustomFilterDefinition extends Filter {

    public static final AttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder("class-name", ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder("module", ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final PropertiesAttributeDefinition PARAMETERS = new PropertiesAttributeDefinition.Builder("parameters", true)
            .setAllowNull(true)
            .setWrapXmlElement(false)
            .setXmlName("param")
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final CustomFilterDefinition INSTANCE = new CustomFilterDefinition();

    private CustomFilterDefinition() {
        super("custom-filter");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(CLASS_NAME, MODULE, PARAMETERS);
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        String className = model.get(CLASS_NAME.getName()).asString();
        String moduleName = model.get(MODULE.getName()).asString();
        Map<String, String> params = unwrap(model);
        UndertowLogger.ROOT_LOGGER.debugf("Creating http handler %s from module %s with parameters %s", className, moduleName, params);
        Class<?> clazz = getHandlerClass(className, moduleName);
        ConfiguredHandlerWrapper wrapper = new ConfiguredHandlerWrapper(clazz, params);
        if (predicate != null) {
            return Handlers.predicate(predicate, wrapper.wrap(next), next);
        } else {
            return wrapper.wrap(next);
        }
    }

    @Override
    protected Class[] getConstructorSignature() {
        throw new IllegalStateException(); //should not be used, as the handler is constructed above
    }

    protected Class<?> getHandlerClass(String className, String moduleName) {
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        try {
            Module filterModule = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
            return filterModule.getClassLoader().loadClassLocal(className);
        } catch (ModuleLoadException | ClassNotFoundException e) {
            throw UndertowLogger.ROOT_LOGGER.couldNotLoadHandlerFromModule(className,moduleName,e);
        }
    }

    private Map<String, String> unwrap(final ModelNode model) {
        if (!model.hasDefined(PARAMETERS.getName())) {
            return Collections.emptyMap();
        }
        ModelNode modelProps = model.get(PARAMETERS.getName());
        Map<String, String> props = new HashMap<String, String>();
        for (Property p : modelProps.asPropertyList()) {
            props.put(p.getName(), p.getValue().asString());
        }
        return props;
    }
}
