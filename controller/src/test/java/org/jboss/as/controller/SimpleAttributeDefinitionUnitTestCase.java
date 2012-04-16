/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.controller;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Tests of {@link SimpleAttributeDefinition}
 */
public class SimpleAttributeDefinitionUnitTestCase {
    
    static enum TestEnum {
        A, B, C
    }
    
    @Test
    public void testAllowedValues() {
        SimpleAttributeDefinition ad = new SimpleAttributeDefinitionBuilder("test", ModelType.STRING)
                .setValidator(new EnumValidator<TestEnum>(TestEnum.class, false, false, TestEnum.A, TestEnum.B))
                .build();

        ParameterValidator pv = ad.getValidator();
        Assert.assertTrue(pv instanceof AllowedValuesValidator);
        List<ModelNode> allowed = ((AllowedValuesValidator) pv).getAllowedValues();
        Assert.assertNotNull(allowed);
        Assert.assertEquals(2, allowed.size());
        Assert.assertTrue(allowed.contains(new ModelNode("A")));
        Assert.assertTrue(allowed.contains(new ModelNode("B")));   
        
        ad = new SimpleAttributeDefinitionBuilder("test", ModelType.STRING)
                .build();        
        
        pv = ad.getValidator();
        Assert.assertTrue(pv instanceof AllowedValuesValidator);
        allowed = ((AllowedValuesValidator) pv).getAllowedValues();
        Assert.assertNull(allowed);
    }

    @Test
    public void testMinMax() {
        SimpleAttributeDefinition ad = new SimpleAttributeDefinitionBuilder("test", ModelType.INT)
                .setValidator(new IntRangeValidator(5, 10, false, false))
                .build();

        ParameterValidator pv = ad.getValidator();
        Assert.assertTrue(pv instanceof MinMaxValidator);
        Long min = ((MinMaxValidator) pv).getMin();
        Assert.assertNotNull(min);
        Assert.assertEquals(Long.valueOf(5), min);
        Long max = ((MinMaxValidator) pv).getMax();
        Assert.assertNotNull(max);
        Assert.assertEquals(Long.valueOf(10), max);

        ad = new SimpleAttributeDefinitionBuilder("test", ModelType.BOOLEAN)
                .build();

        pv = ad.getValidator();
        Assert.assertTrue(pv instanceof MinMaxValidator);
        min = ((MinMaxValidator) pv).getMin();
        Assert.assertNull(min);
        max = ((MinMaxValidator) pv).getMax();
        Assert.assertNull(max);
    }
}
