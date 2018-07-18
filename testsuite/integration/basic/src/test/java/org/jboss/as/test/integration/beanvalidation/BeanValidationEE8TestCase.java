/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.beanvalidation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * A test validating the Bean Validation 2.0 support.
 *
 * @author <a href="mailto:guillaume@hibernate.org">Guillaume Smet</a>
 */
@RunWith(Arquillian.class)
public class BeanValidationEE8TestCase {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "beanvalidation-ee8-test-case.war");
        war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");

        return war;
    }

    @Test
    public void testMapKeySupport() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<MapKeyBean>> violations = validator.validate(MapKeyBean.valid());
        Assert.assertTrue(violations.isEmpty());

        violations = validator.validate(MapKeyBean.invalid());
        Assert.assertEquals(1, violations.size());

        ConstraintViolation<MapKeyBean> violation = violations.iterator().next();
        Assert.assertEquals(NotNull.class, violation.getConstraintDescriptor().getAnnotation().annotationType());
    }

    @Test
    public void testValueExtractor() {
        Validator validator = Validation.byDefaultProvider().configure()
                .addValueExtractor(new ContainerValueExtractor())
                .buildValidatorFactory()
                .getValidator();

        Set<ConstraintViolation<ContainerBean>> violations = validator.validate(ContainerBean.valid());
        Assert.assertTrue(violations.isEmpty());

        violations = validator.validate(ContainerBean.invalid());
        Assert.assertEquals(1, violations.size());

        ConstraintViolation<ContainerBean> violation = violations.iterator().next();
        Assert.assertEquals(NotNull.class, violation.getConstraintDescriptor().getAnnotation().annotationType());
    }

    private static class MapKeyBean {

        private Map<@NotNull String, String> mapProperty;

        private static MapKeyBean valid() {
            MapKeyBean validatedBean = new MapKeyBean();
            validatedBean.mapProperty = new HashMap<>();
            validatedBean.mapProperty.put("Paul Auster", "4 3 2 1");
            return validatedBean;
        }

        private static MapKeyBean invalid() {
            MapKeyBean validatedBean = new MapKeyBean();
            validatedBean.mapProperty = new HashMap<>();
            validatedBean.mapProperty.put(null, "4 3 2 1");
            return validatedBean;
        }
    }

    private static class ContainerBean {

        @NotNull
        private Container containerProperty;

        private static ContainerBean valid() {
            ContainerBean validatedBean = new ContainerBean();
            validatedBean.containerProperty = new Container("value");
            return validatedBean;
        }

        private static ContainerBean invalid() {
            ContainerBean validatedBean = new ContainerBean();
            validatedBean.containerProperty = new Container(null);
            return validatedBean;
        }
    }

    private static class Container {

        private String value;

        private Container(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    @UnwrapByDefault
    private class ContainerValueExtractor implements ValueExtractor<@ExtractedValue(type = String.class) Container> {

        @Override
        public void extractValues(Container originalValue, ValueReceiver receiver) {
            receiver.value(null, originalValue.getValue());
        }
    }
}
