package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static java.lang.annotation.ElementType.FIELD;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import junit.framework.Assert;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.EmailDef;
import org.hibernate.validator.messageinterpolation.ValueFormatterMessageInterpolator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * Tests that message interpolation works correctly for hibernate validator
 * 
 * 
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class MessageInterpolationValidationTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testmessageinterpolationvalidation.war");
        war.addPackage(MessageInterpolationValidationTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testCustomMessageInterpolation() {

        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        Assert.assertNotNull(configuration);
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.type(Employee.class).property("firstName", FIELD)
                .constraint(new EmailDef().message("Invalid Email!You have entered:--\\{${validatedValue}\\}"));

        final MessageInterpolator messageInterpolator = new ValueFormatterMessageInterpolator();
        configuration.messageInterpolator(messageInterpolator);
        configuration.addMapping(mapping);

        ValidatorFactory factory = configuration.buildValidatorFactory();
        Validator validator = factory.getValidator();

        Employee emp = new Employee();
        // create employee
        emp.setEmpId("M1234");
        emp.setFirstName("MADHUMITA");
        String email = "MADHUMITA";

        Set<ConstraintViolation<Employee>> constraintViolations = validator.validate(emp);

        Assert.assertEquals("Wrong number of constraints", constraintViolations.size(), 1);
        Assert.assertEquals("Invalid Email!You have entered:--{" + email + "}", constraintViolations.iterator().next()
                .getMessage());

    }
}
