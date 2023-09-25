/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.beanvalidation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * A test validating the Jakarta Bean Validation 2.0 support.
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
