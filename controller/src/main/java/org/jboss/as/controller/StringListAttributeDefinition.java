package org.jboss.as.controller;

import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class StringListAttributeDefinition extends PrimitiveListAttributeDefinition {

    public StringListAttributeDefinition(String name, String xmlName, boolean allowNull) {
        super(name, xmlName, allowNull, ModelType.STRING);
    }
}
