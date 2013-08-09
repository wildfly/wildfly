package org.jboss.as.logging.validators;

import java.util.regex.Pattern;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A validator that accepts a pattern to test the {@link org.jboss.dmr.ModelNode#asString() string value parameter.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RegexValidator extends ModelTypeValidator {
    private final Pattern pattern;

    public RegexValidator(final ModelType type, final boolean nullable, final boolean allowExpressions, final String pattern) {
        super(type, nullable, allowExpressions);
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            final String stringValue = value.asString();
            if (!pattern.matcher(stringValue).matches()) {
                throw new OperationFailedException("Does not match pattern");
            }
        }
    }
}
