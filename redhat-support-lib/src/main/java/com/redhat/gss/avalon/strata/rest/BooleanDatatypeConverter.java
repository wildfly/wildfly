package com.redhat.gss.avalon.strata.rest;

/**
 * This custom datatype converter is used to parse booleans with more leniency
 * with regard to case sensitivity.
 * <p>
 * The following are examples of acceptable value of <code>true</code>:
 *
 * <ul>
 * <li>true</li>
 * <li>True</li>
 * <li>TRUE</li>
 * </ul>
 *
 * This converter is referenced in strata.xsd and is called during
 * unmarshalling.
 *
 * @author Brian Dill
 *
 */
public class BooleanDatatypeConverter {

    /**
     * Converts the string argument into a boolean value.
     *
     * @param lexicalXSDBoolean
     *            A string containing lexical representation of xsd:boolean.
     * @return A boolean value represented by the string argument.
     */
    public static Boolean parseBoolean(String lexicalXSDBoolean) {
        if (lexicalXSDBoolean != null
                && lexicalXSDBoolean.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

}
