package org.jboss.as.subsystem.test;

import org.jboss.as.controller.parsing.ExtensionParsingContext;

/**
 * Allows you to additionally initialize the parsers
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface AdditionalParsers {
    /**
     * Allows you to add subsystems parsers
     *
     * @param context the extension parsing context to add your subsystem parser to
     */
    void addParsers(ExtensionParsingContext context);
}
