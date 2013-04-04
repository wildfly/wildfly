package org.jboss.as.undertow;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ServerDefinition extends SimplePersistentResourceDefinition {
    static final ServerDefinition INSTANCE = new ServerDefinition();
    static final SimpleAttributeDefinition DEFAULT_HOST = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_HOST, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("default-host"))
            .build();
    static final SimpleAttributeDefinition SERVLET_CONTAINER = new SimpleAttributeDefinitionBuilder(Constants.SERVLET_CONTAINER, ModelType.STRING)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("default"))
            .build();
    static final List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(DEFAULT_HOST, SERVLET_CONTAINER);
    static final List<SimplePersistentResourceDefinition> CHILDREN = Arrays.asList(
            AJPListenerResourceDefinition.INSTANCE,
            HttpListenerResourceDefinition.INSTANCE,
            HttpsListenerResourceDefinition.INSTANCE,
            HostDefinition.INSTANCE);

    public ServerDefinition() {
        super(UndertowExtension.SERVER_PATH, UndertowExtension.getResolver(Constants.SERVER), new ServerAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        //noinspection unchecked
        return CHILDREN;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        super.registerChildren(registration);
        for (Handler handler : HandlerFactory.getHandlers()) {
            registration.registerSubModel(handler);
        }
    }

    @Override
    public void parseChildren(XMLExtendedStreamReader reader, PathAddress address, List<ModelNode> list) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (reader.getLocalName()) {
                case Constants.HTTP_LISTENER: {
                    HttpListenerResourceDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                case Constants.HTTPS_LISTENER: {
                    HttpsListenerResourceDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                case Constants.AJP_LISTENER: {
                    AJPListenerResourceDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                case Constants.HANDLERS: {
                    HandlerFactory.parseHandlers(reader, address, list);
                    break;
                }
                case Constants.HOST: {
                    HostDefinition.INSTANCE.parse(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    @Override
    public void persistChildren(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        HttpListenerResourceDefinition.INSTANCE.persist(writer, config);
        HttpsListenerResourceDefinition.INSTANCE.persist(writer, config);
        AJPListenerResourceDefinition.INSTANCE.persist(writer, config);
        HandlerFactory.persistHandlers(writer, config, true);
        HostDefinition.INSTANCE.persist(writer, config);
    }

}
