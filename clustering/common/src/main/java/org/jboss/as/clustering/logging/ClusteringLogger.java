/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.logging;

import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.projectodd.vdx.core.ErrorType;
import org.projectodd.vdx.core.ValidationError;
import org.projectodd.vdx.core.XMLStreamValidationException;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLCOM", length = 4)
public interface ClusteringLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.jboss.as.clustering";

    /**
     * The root logger.
     */
    ClusteringLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ClusteringLogger.class, ROOT_LOGGER_CATEGORY);

    @Message(id = 1, value = "%2$g is not a valid value for parameter %1$s. The value must be %3$s %4$g")
    OperationFailedException parameterValueOutOfBounds(String name, double value, String relationalOperator, double bound);

    @Message(id = 2, value = "Failed to close %s")
    @LogMessage(level = WARN)
    void failedToClose(@Cause Throwable cause, Object value);

    @Message(id = 3, value = "The following attributes do not support negative values: %s")
    String attributesDoNotSupportNegativeValues(Set<String> attributes);

    @Message(id = 4, value = "The following attributes do not support zero values: %s")
    String attributesDoNotSupportZeroValues(Set<String> attributes);

    @Message(id = 5, value = "Legacy host does not support multiple values for attributes: %s")
    String rejectedMultipleValues(Set<String> attributes);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "The '%s' attribute of the '%s' element is no longer supported and will be ignored")
    void attributeIgnored(String attribute, String element);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "The '%s' element is no longer supported and will be ignored")
    void elementIgnored(String element);

    @Message(id = 8, value = "%s:%s operation is only supported in admin-only mode.")
    OperationFailedException operationNotSupportedInNormalServerMode(String address, String operation);

    default void attributeIgnored(QName elementName, QName attributeName) {
        this.attributeIgnored(attributeName.getLocalPart(), elementName.getLocalPart());
    }

    default void elementIgnored(QName elementName) {
        this.elementIgnored(elementName.toString());
    }

    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    @Message(id = 520, value = "Element '%s' already defines attribute: %s")
    IllegalArgumentException duplicateAttributes(QName elementName, QName attributeName);

    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    @Message(id = 521, value = "XML model group already defines element: %s")
    IllegalArgumentException duplicateElements(QName elementName);

    @Message(id = 522, value = "XML choice already defines resource: %s")
    IllegalArgumentException duplicateElement(PathElement path);

    @Message(id = 523, value = "XML choice already defines resource: %s")
    IllegalArgumentException duplicatePathElement(PathElement path);

    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    @Message(id = 524, value = "Element(s) '%s' must occur at least %d time(s)")
    String minOccursNotReached(Set<QName> elements, int minOccurs);

    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    @Message(id = 525, value = "Element(s) '%s' may not occur more than %d time(s)")
    String maxOccursExceeded(Set<QName> elements, int maxOccurs);

    /**
     * Creates an exception reporting that a given element did not appear a sufficient number of times.
     * @param reader the stream reader
     * @param elementName the element name
     * @return a validation exception
     */
    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    default XMLStreamException minOccursNotReached(XMLExtendedStreamReader reader, Set<QName> choices, XMLCardinality cardinality) {
        XMLStreamException e = new XMLStreamException(this.minOccursNotReached(choices, cardinality.getMinOccurs()), reader.getLocation());
        return this.createValidationException(e, ErrorType.REQUIRED_ELEMENT_MISSING, choices);
    }

    /**
     * Creates an exception reporting that a given element appeared too many times.
     * @param reader the stream reader
     * @param elementName the element name
     * @return a validation exception
     */
    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    default XMLStreamException maxOccursExceeded(XMLExtendedStreamReader reader, Set<QName> choices, XMLCardinality cardinality) {
        XMLStreamException e = new XMLStreamException(this.maxOccursExceeded(choices, cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE)), reader.getLocation());
        return this.createValidationException(e, ErrorType.DUPLICATE_ELEMENT, choices);
    }

    // TODO Relocate to org.jboss.as.controller.parsing.ParseUtils.
    default XMLStreamValidationException createValidationException(XMLStreamException e, ErrorType type, Set<QName> choices) {
        ValidationError error = ValidationError.from(e, type);
        Iterator<QName> names = choices.iterator();
        if (names.hasNext()) {
            error.element(names.next());
        }
        if (names.hasNext()) {
            Set<String> alternatives = new TreeSet<>();
            do {
                alternatives.add(names.next().getLocalPart());
            } while (names.hasNext());
            error.alternatives(alternatives);
        }
        return new XMLStreamValidationException(e.getMessage(), error, e);
    }
}
