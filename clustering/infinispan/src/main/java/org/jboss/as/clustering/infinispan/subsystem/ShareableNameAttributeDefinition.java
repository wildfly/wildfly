package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ResourceBundle;

import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An AttributeDefinition implementation which allows specifying a CLI name used generate all attribute and operation
 * descriptions. Useful when all AttributeDefinitions are global. Allows defining attributes which have different model keys
 * in the model, but the same attribute or parameter name for operations.
 *
 * e.g. CACHE_MODE and TXN_MODE which both need to be referenced as "mode" in operation interfaces.
 *
 * TODO: remove such attribute definitions when we move to resources.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ShareableNameAttributeDefinition extends SimpleAttributeDefinition {

    private final String cliName ;

    public ShareableNameAttributeDefinition(final String name, final String cliName, final ModelType type, final boolean allowNull) {
        super(name, name, null, type, allowNull, false, null);
        this.cliName = cliName ;
    }

    public ShareableNameAttributeDefinition(final String name, final String cliName, final ModelType type, final boolean allowNull, ParameterCorrector corrector, ParameterValidator validator) {
        super(name, name, null, type, allowNull, false, MeasurementUnit.NONE, corrector, validator, null, null);
        this.cliName = cliName ;
    }

    public String getCliName() {
        return cliName;
    }

    /**
     * Creates a returns a basic model node describing the attribute, after attaching it to the given overall resource
     * description model node.  The node describing the attribute is returned to make it easy to perform further
     * modification.
     *
     * @param bundle resource bundle to use for text descriptions
     * @param prefix prefix to prepend to the attribute name key when looking up descriptions
     * @param resourceDescription  the overall resource description
     * @return  the attribute description node
     */
    public ModelNode addResourceAttributeDescription(final ResourceBundle bundle, final String prefix, final ModelNode resourceDescription) {
        final ModelNode attr = getNoTextDescription(false);
        attr.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
        final ModelNode result = resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES, getCliName()).set(attr);
        return result;
    }

    /**
     * Creates a returns a basic model node describing a parameter that sets this attribute, after attaching it to the
     * given overall operation description model node.  The node describing the parameter is returned to make it easy
     * to perform further modification.
     *
     * @param bundle resource bundle to use for text descriptions
     * @param prefix prefix to prepend to the attribute name key when looking up descriptions
     * @param operationDescription  the overall resource description
     * @return  the attribute description node
     */
    public ModelNode addOperationParameterDescription(final ResourceBundle bundle, final String prefix, final ModelNode operationDescription) {
        final ModelNode param = getNoTextDescription(true);
        param.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
        final ModelNode result = operationDescription.get(ModelDescriptionConstants.REQUEST_PROPERTIES, getCliName()).set(param);
        return result;
    }
}
