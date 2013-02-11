package org.jboss.as.subsystem.test;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.staxmapper.XMLMapper;

/**
 * Allows you to initialize additional subsystem parsers beyond the default one associated
 * with the subsystem under test.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AdditionalParsers {

    /**
     * Allows you to {@link ExtensionRegistry#getExtensionParsingContext(String, XMLMapper) access an
     * {@link ExtensionParsingContext} and use it to add subsystem parsers.
     *
     * @param registry the extension registry
     * @param xmlMapper the XMLMapper that will be used for overall parsing
     */
    protected void addParsers(ExtensionRegistry registry, XMLMapper xmlMapper) {

    }
}
