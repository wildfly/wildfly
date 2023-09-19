package org.wildfly.ee.feature.pack.layer.tests.bean.validation;

import jakarta.validation.constraints.Email;

public class BeanValidationAnnotationInRootPackageUsage {
    @Email
    String s;
}
