package org.jboss.as.jacorb;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.CLIENT;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.IDENTITY;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.SECURITY;

/**
 * @author Stuart Douglas
 */
public class SecurityInterceptorTransformer implements OperationTransformer, ResourceTransformer, ChainedResourceTransformerEntry {

    public static final SecurityInterceptorTransformer INSTANCE = new SecurityInterceptorTransformer();

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        final ModelNode ret = operation.clone();
        if (operation.get(OP).asString().equals(WRITE_ATTRIBUTE_OPERATION)) {
            //write attribute operation
            if (operation.get(NAME).equals(SECURITY)) {
                final String security = operation.get(VALUE).asString();
                if (security.equals(CLIENT) || security.equals(IDENTITY)) {
                    ret.get(VALUE).set("on");
                }
            }
        } else if (operation.get(OP).asString().equals(ADD)) {
            doTransform(ret);
        }
        return new TransformedOperation(ret, OperationResultTransformer.ORIGINAL_RESULT);
    }

    private void doTransform(final ModelNode operation) {
        if (operation.has(SECURITY)) {
            final String security = operation.get(SECURITY).asString();
            if (security.equals(CLIENT) || security.equals(IDENTITY)) {
                operation.get(SECURITY).set("on");
            }

        }
    }

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {
        doTransform(resource.getModel());
    }

    @Override
    public void transformResource(final ChainedResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {
        doTransform(resource.getModel());
    }
}
