package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * The operation transformer. For basic implementations {@see AbstractOperationTransformer}.
 *
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface OperationTransformer {

    /**
     * Transform the operation.
     *
     * @param context the operation context
     * @param address the path address
     * @param operation the operation
     * @return the transformed operation
     * @throws OperationFailedException
     */
    TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException;

    public class TransformedOperation implements OperationResultTransformer {

        private final ModelNode transformedOperation;
        private final OperationResultTransformer resultTransformer;

        public TransformedOperation(ModelNode transformedOperation, OperationResultTransformer resultTransformer) {
            this.transformedOperation = transformedOperation;
            this.resultTransformer = resultTransformer;
        }

        public ModelNode getTransformedOperation() {
            return transformedOperation;
        }

        public OperationResultTransformer getResultTransformer() {
            return resultTransformer;
        }

        @Override
        public ModelNode transformResult(final ModelNode result) {
            return resultTransformer.transformResult(result);
        }

    }

}
