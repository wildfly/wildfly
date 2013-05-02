package org.jboss.as.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.undertow.filters.FilterRefDefinition;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationDefinition extends SimplePersistentResourceDefinition {
    static final AttributeDefinition HANDLER = new SimpleAttributeDefinitionBuilder(Constants.HANDLER, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new StringLengthValidator(1))
            .build();
    private static final List<? extends SimplePersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(Arrays.asList(FilterRefDefinition.INSTANCE));
    static final LocationDefinition INSTANCE = new LocationDefinition();


    private LocationDefinition() {
        super(UndertowExtension.PATH_LOCATION,
                UndertowExtension.getResolver(Constants.HOST, Constants.LOCATION),
                LocationAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singleton(HANDLER);
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
