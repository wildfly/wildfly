/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@Priority(101)
public class HighPriorityMyStringConverter2 implements Converter<MyString> {

    @Override
    public MyString convert(String value) {
        return !value.isEmpty() ? MyString.from("Property converted by HighPriorityStringConverter2") : null;
    }
}
