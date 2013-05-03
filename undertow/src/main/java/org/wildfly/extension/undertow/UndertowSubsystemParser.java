package org.wildfly.extension.undertow;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final UndertowSubsystemParser INSTANCE = new UndertowSubsystemParser();

    private UndertowSubsystemParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(UndertowRootDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());//this is bit of workaround for SPRD to work properly
        UndertowRootDefinition.INSTANCE.persist(writer, model, Namespace.CURRENT.getUriString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        UndertowRootDefinition.INSTANCE.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}

