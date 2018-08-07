package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@Priority(102)
public class Return102Converter implements Converter<Integer> {

    @Override
    public Integer convert(String value) {
        return 102;
    }
}
