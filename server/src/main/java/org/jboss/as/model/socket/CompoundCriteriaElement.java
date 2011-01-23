/**
 *
 */
package org.jboss.as.model.socket;

import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.interfaces.AnyInterfaceCriteria;
import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.NotInterfaceCriteria;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that if a network interface satisfies either any or none of a set of nested
 * criteria, it may be used. Whether the test is for any or none depends on the
 * <code>isAny</code> parameter passed to the constructor.
 *
 * @author Brian Stansberry
 */
public class CompoundCriteriaElement extends AbstractInterfaceCriteriaElement<CompoundCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;
    private final Set<AbstractInterfaceCriteriaElement<?>> interfaceCriteria;

    public CompoundCriteriaElement(Set<AbstractInterfaceCriteriaElement<?>> interfaceCriteria, boolean isAny) {
        super(isAny ? Element.ANY : Element.NOT);

        Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>(interfaceCriteria.size());
        for (AbstractInterfaceCriteriaElement<?> element : interfaceCriteria) {
            criteria.add(element.getInterfaceCriteria());
        }

        InterfaceCriteria ours = isAny ? new AnyInterfaceCriteria(criteria) : new NotInterfaceCriteria(criteria);

        this.interfaceCriteria = interfaceCriteria;
        setInterfaceCriteria(ours);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        synchronized (interfaceCriteria) {
            for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria) {
                streamWriter.writeStartElement(criteria.getElement().getLocalName());
                criteria.writeContent(streamWriter);
            }
        }
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<CompoundCriteriaElement> getElementClass() {
        return CompoundCriteriaElement.class;
    }
}
