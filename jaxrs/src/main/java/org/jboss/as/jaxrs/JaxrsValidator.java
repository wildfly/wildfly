package org.jboss.as.jaxrs;

import java.util.regex.Pattern;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.dmr.ModelNode;

public class JaxrsValidator implements ParameterValidator {

    private static final String SPACE = "\\s*";
    private static final String SPACE_COMMA_SPACE = SPACE + "," + SPACE;
    private static final String SPACE_NONWHITESPACE_SPACE = SPACE + "\\S+" + SPACE;
    private static final String JAVA_SIMPLE_ID = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    private static final String JAVA_ID = JAVA_SIMPLE_ID + "(\\." + JAVA_SIMPLE_ID + ")*";
    private static final String MAP_ELEMENT = SPACE_NONWHITESPACE_SPACE + ":" + SPACE_NONWHITESPACE_SPACE;
    private static final String EMPTY = "\\s*";

    private static final Pattern JAVA_ID_LIST_PATTERN = Pattern.compile(EMPTY + "|(" + SPACE + JAVA_ID + SPACE + "(" + SPACE_COMMA_SPACE + JAVA_ID + SPACE + ")*)");
    private static final Pattern MAP_PATTERN = Pattern.compile(EMPTY + "|(" + MAP_ELEMENT + "(" + SPACE_COMMA_SPACE + MAP_ELEMENT + ")*)");

    public static JaxrsValidator LIST_VALIDATOR_INSTANCE = new JaxrsValidator(JAVA_ID_LIST_PATTERN);
    public static JaxrsValidator MAP_VALIDATOR_INSTANCE = new JaxrsValidator(MAP_PATTERN);

    private Pattern pattern;

    JaxrsValidator(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
       if (pattern.matcher(value.asString()).matches()) {
          return;
       }
       throw new OperationFailedException(JaxrsLogger.JAXRS_LOGGER.illegalArgument(parameterName, value.asString()));
    }
 }