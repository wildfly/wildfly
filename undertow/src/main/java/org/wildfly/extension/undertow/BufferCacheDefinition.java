package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BufferCacheDefinition extends SimplePersistentResourceDefinition {
    protected static final SimpleAttributeDefinition BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_SIZE, ModelType.INT)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(0, false, true))
            .build();
    protected static final SimpleAttributeDefinition BUFFERS_PER_REGION = new SimpleAttributeDefinitionBuilder(Constants.BUFFERS_PER_REGION, ModelType.INT)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(0, false, true))
            .build();
    protected static final SimpleAttributeDefinition MAX_REGIONS = new SimpleAttributeDefinitionBuilder(Constants.MAX_REGIONS, ModelType.INT)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(0, false, true))
            .build();
    static final BufferCacheDefinition INSTANCE = new BufferCacheDefinition();
    private static final List<SimpleAttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(BUFFER_SIZE, BUFFERS_PER_REGION, MAX_REGIONS));

    private BufferCacheDefinition() {
        super(UndertowExtension.PATH_BUFFER_CACHE,
                UndertowExtension.getResolver(Constants.BUFFER_CACHE),
                BufferCacheAdd.INSTANCE,
                new ServiceRemoveStepHandler(BufferCacheService.SERVICE_NAME, BufferCacheAdd.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) ATTRIBUTES;
    }

    @Override
    public String getXmlWrapperElement() {
        return Constants.BUFFER_CACHES;
    }
}
