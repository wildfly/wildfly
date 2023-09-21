package org.wildfly.ee.feature.pack.layer.tests.bean.validation;

import jakarta.validation.groups.ConvertGroup;

public class BeanValidationAnnotationInGroupsPackageUsage {
    @ConvertGroup(to=BeanValidationAnnotationInGroupsPackageUsage.class)
    String convertGroup;
}
