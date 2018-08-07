package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@Priority(101)
public class HighPriorityStringConverter2 implements Converter<String> {

    @Override
    public String convert(String value) {
        return "Property converted by HighPriorityStringConverter2";
    }
}
