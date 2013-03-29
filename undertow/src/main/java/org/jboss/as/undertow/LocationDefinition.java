package org.jboss.as.undertow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationDefinition extends SimplePersistentResourceDefinition {

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
        return Collections.emptySet();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler writer = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writer);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        for (Handler handler : HandlerFactory.getHandlers()) {
            resourceRegistration.registerSubModel(handler);
        }
    }

    @Override
    public void parseChildren(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        HandlerFactory.parseHandlers(reader, parentAddress, list);
    }

    @Override
    public void persistChildren(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        HandlerFactory.persistHandlers(writer, model, false);
    }
}
