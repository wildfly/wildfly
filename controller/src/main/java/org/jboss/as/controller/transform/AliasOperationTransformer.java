package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationTransformer} transforming the operation address only.
 *
 * @author Emanuel Muckenhuber
 */
public class AliasOperationTransformer implements OperationTransformer {

    public interface AddressTransformer {

        /**
         * Transform an address.
         *
         * @param address the address to transform
         * @return the transformed address
         */
        PathAddress transformAddress(PathAddress address);

    }

    private final AddressTransformer transformer;
    protected AliasOperationTransformer(AddressTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode original) {
        final ModelNode operation = original.clone();
        final PathAddress transformedAddress = transformer.transformAddress(address);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(transformedAddress.toModelNode());
        return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }

    /**
     * Replace the last element of an address with a static path element.
     *
     * @param element the path element
     * @return the operation address transformer
     */
    public static AliasOperationTransformer replaceLastElement(final PathElement element) {
        return create(new AddressTransformer() {
            @Override
            public PathAddress transformAddress(final PathAddress original) {
                final PathAddress address = original.subAddress(0, original.size() -1);
                return address.append(element);
            }
        });
    }

    public static AliasOperationTransformer create(final AddressTransformer transformer) {
        return new AliasOperationTransformer(transformer);
    }

}
