package org.jboss.as.undertow;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostDefinition extends SimplePersistentResourceDefinition {
    protected static final StringListAttributeDefinition ALIAS = new StringListAttributeDefinition.Builder(Constants.ALIAS)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();
    static final HostDefinition INSTANCE = new HostDefinition();
    private static final Collection ATTRIBUTES = Collections.singleton(ALIAS);


    private HostDefinition() {
        super(UndertowExtension.HOST_PATH, UndertowExtension.getResolver(Constants.HOST),
                HostAdd.INSTANCE,
                new HostRemove());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return ATTRIBUTES;
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
        resourceRegistration.registerSubModel(LocationDefinition.INSTANCE);
    }

    public void parse(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        String aliases = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            switch (reader.getAttributeLocalName(i)) {
                case Constants.NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case Constants.ALIAS:
                    aliases = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Constants.NAME);
        }

        PathAddress address = parentAddress.append(Constants.HOST, name);
        ModelNode op = Util.createAddOperation(address);
        if (aliases != null) {
            for (String alias : aliases.split(",")) {
            ALIAS.parseAndAddParameterElement(alias, op, reader);
        }
        }
        list.add(op);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (reader.getLocalName()) {
                case Constants.HANDLERS: {
                    HandlerFactory.parseHandlers(reader, address, list);
                    break;
                }
                case Constants.LOCATION: {
                    LocationDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    public void persist(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        if (!model.hasDefined(Constants.HOST)) {
            return;
        }
        for (final Property hostProp : model.get(Constants.HOST).asPropertyList()) {
            writer.writeStartElement(Constants.HOST);
            ModelNode host = hostProp.getValue();
            writer.writeAttribute(Constants.NAME, hostProp.getName());
            StringBuilder aliases = new StringBuilder();
            if (host.hasDefined(ALIAS.getName())) {
                for (ModelNode p : host.get(ALIAS.getName()).asList()) {
                    aliases.append(p.asString()).append(", ");
                }
            }
            if (aliases.length() > 3) {
                aliases.setLength(aliases.length() - 2);
            }
            if (aliases.length()>0){
                writer.writeAttribute(Constants.ALIAS, aliases.toString());
            }
            HandlerFactory.persistHandlers(writer, host, true);
            LocationDefinition.INSTANCE.persist(writer, host);
            writer.writeEndElement();
        }
    }
}
