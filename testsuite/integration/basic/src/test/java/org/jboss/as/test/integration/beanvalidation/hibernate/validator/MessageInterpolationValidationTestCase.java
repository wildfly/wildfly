package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that message interpolation works correctly for Hibernate Validator.
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class MessageInterpolationValidationTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testmessageinterpolationvalidation.war");
        war.addPackage(MessageInterpolationValidationTestCase.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");
        return war;
    }

    @Test
    public void testCustomMessageInterpolation() {

        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        assertNotNull(configuration);

        final MessageInterpolator messageInterpolator = new CustomMessageInterpolator();
        configuration.messageInterpolator(messageInterpolator);

        ValidatorFactory factory = configuration.buildValidatorFactory();
        Validator validator = factory.getValidator();

        // create employee
        Employee emp = new Employee();
        emp.setEmail("MADHUMITA");

        Set<ConstraintViolation<Employee>> constraintViolations = validator.validate(emp);

        assertEquals("Wrong number of constraints", constraintViolations.size(), 1);
        assertEquals(CustomMessageInterpolator.MESSAGE, constraintViolations.iterator().next().getMessage());
    }

    private static class CustomMessageInterpolator implements MessageInterpolator {

        public static final String MESSAGE = "Message created by custom interpolator";

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return MESSAGE;
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return MESSAGE;
        }
    }
}
