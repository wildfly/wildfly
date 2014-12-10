package org.jboss.as.cli.handlers.ifelse;

import org.jboss.dmr.ModelNode;

/**
 * Created by joe on 11/28/14.
 */
public abstract class SameTypeOperation extends ComparisonOperation {

    SameTypeOperation(String name) {
        super(name);
    }

    @Override
    protected boolean compare(Object left, Object right) {

        if(((ModelNode) left).getType() != ((ModelNode)right).getType()) {
            return false;
        }

        return doCompare(left, right);
    }

    protected abstract boolean doCompare(Object left, Object right);
}
