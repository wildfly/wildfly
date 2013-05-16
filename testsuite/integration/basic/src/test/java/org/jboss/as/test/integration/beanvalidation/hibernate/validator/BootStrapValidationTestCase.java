package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.internal.constraintvalidators.NotNullValidator;
import org.hibernate.validator.internal.engine.ConstraintValidatorFactoryImpl;

/**
 * Tests that bootstrapping works correctly for hibernate validator
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class BootStrapValidationTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testbootstrapvalidation.war");
        war.addPackage(BootStrapValidationTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testBootstrapAsServiceDefault() {
        HibernateValidatorFactory factory = (HibernateValidatorFactory) Validation.buildDefaultValidatorFactory();
        Assert.assertNotNull(factory);
    }

    @Test
    public void testCustomConstraintValidatorFactory() {

        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        Assert.assertNotNull(configuration);

        ValidatorFactory factory = configuration.buildValidatorFactory();
        Validator validator = factory.getValidator();

        Employee emp = new Employee();
        // create employee
        emp.setEmpId("M1234");
        emp.setFirstName("MADHUMITA");
        emp.setLastName("SADHUKHAN");
        emp.setEmail("madhu@redhat.com");

        Set<ConstraintViolation<Employee>> constraintViolations = validator.validate(emp);
        Assert.assertEquals("Wrong number of constraints", constraintViolations.size(), 0);

        // get a new factory using a custom configuration
        configuration = (HibernateValidatorConfiguration) Validation.byDefaultProvider().configure();
        configuration.constraintValidatorFactory(new ConstraintValidatorFactory() {

            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key == NotNullValidator.class) {
                    return (T) new ErroneousNotNullValidator();
                }
                return new ConstraintValidatorFactoryImpl().getInstance(key);
            }
        });

        factory = configuration.buildValidatorFactory();
        validator = factory.getValidator();
        constraintViolations = validator.validate(emp);
        Assert.assertEquals("Wrong number of constraints", constraintViolations.size(), 1);
    }

    /**
     * Ensure that including a cross-site script attack triggers the @SafeHtml validation rule
     */
    @Test
    public void testSafeHTML() {

        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        Assert.assertNotNull(configuration);

        ValidatorFactory factory = configuration.buildValidatorFactory();
        Validator validator = factory.getValidator();

        Employee emp = new Employee();
        // create employee
        emp.setEmpId("M1234");
        emp.setFirstName("Joe");
        emp.setLastName("Cocker");
        emp.setEmail("none@jboss.org");
        emp.setWebsite("<script> Cross-site scripting http://en.wikipedia.org/wiki/Joe_Cocker <script/>.");

        Set<ConstraintViolation<Employee>> constraintViolations = validator.validate(emp);
        Assert.assertEquals("Wrong number of constraints", constraintViolations.size(), 1);

    }

    class ErroneousNotNullValidator implements ConstraintValidator<NotNull,Object> {
        @Override
        public void initialize(NotNull parameters) {
           }
        @Override
        public boolean isValid(Object obj, ConstraintValidatorContext constraintValidatorContext) {

            if (obj != null) {
                String var = ((String) obj);

                if (var.length() > 0 && var.length() < 10 && var.matches("^[0-9]+$")) {

                    return true;
                } else {

                    return false;

                }
            } else {

                return false;

            }
        }
    }
}
