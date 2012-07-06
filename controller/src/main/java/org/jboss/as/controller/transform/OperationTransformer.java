package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 *
 * @deprecated experimental interface; may be removed or change without warning. Should not be used outside the main JBoss AS codebase
*/
@Deprecated
public interface OperationTransformer {

    /**
     * Transform the operation.
     *
     * @param context the operation context
     * @param address the path address
     * @param operation the operation
     * @return the transformed operation
     */
    TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation);

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
