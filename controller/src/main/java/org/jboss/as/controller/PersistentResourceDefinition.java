package org.jboss.as.controller;

import java.util.Collection;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public interface PersistentResourceDefinition extends ResourceDefinition {
    Collection<AttributeDefinition> getAttributes();

    List<? extends PersistentResourceDefinition> getChildren();

    String getXmlElementName();

    String getXmlWrapperElement();

    void parse(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException;

    void persist(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException;
}
