package org.jboss.as.weld.services.bootstrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;


/**
 * THIS IS A HACK
 * <p/>
 * It should be removed once HV5 is in the code base
 *
 * @author Stuart Douglas
 */
public class HackValidationExtension implements Extension {

    private final ValidatorFactory factory;

    public HackValidationExtension(final ValidatorFactory factory) {
        this.factory = factory;
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addBean(new ValidatorBean(factory));
        event.addBean(new ValidatorFactoryBean(factory));
    }


    public static final class ValidatorBean implements Bean<Validator>, PassivationCapable {

        private final ValidatorFactory factory;

        public ValidatorBean(final ValidatorFactory factory) {
            this.factory = factory;
        }

        @Override
        public Class<?> getBeanClass() {
            return Validator.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Set<Type> getTypes() {
            final Set<Type> ret = new HashSet<>();
            ret.add(Validator.class);
            ret.add(Object.class);
            return ret;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            final Set<Annotation> ret = new HashSet<>();
            ret.add(DefaultLiteral.INSTANCE);
            ret.add(AnyLiteral.INSTANCE);
            return ret;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Validator create(final CreationalContext<Validator> creationalContext) {
            return factory.getValidator();
        }

        @Override
        public void destroy(final Validator instance, final CreationalContext<Validator> creationalContext) {

        }

        @Override
        public String getId() {
            return HackValidationExtension.class.getName() + ".validator";
        }
    }

    public static final class ValidatorFactoryBean implements Bean<ValidatorFactory>, PassivationCapable {

        private final ValidatorFactory factory;

        public ValidatorFactoryBean(final ValidatorFactory factory) {
            this.factory = factory;
        }

        @Override
        public Class<?> getBeanClass() {
            return Validator.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Set<Type> getTypes() {
            final Set<Type> ret = new HashSet<>();
            ret.add(ValidatorFactory.class);
            ret.add(Object.class);
            return ret;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            final Set<Annotation> ret = new HashSet<>();
            ret.add(DefaultLiteral.INSTANCE);
            ret.add(AnyLiteral.INSTANCE);
            return ret;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public ValidatorFactory create(final CreationalContext<ValidatorFactory> creationalContext) {
            return factory;
        }

        @Override
        public void destroy(final ValidatorFactory instance, final CreationalContext<ValidatorFactory> creationalContext) {

        }

        @Override
        public String getId() {
            return HackValidationExtension.class.getName() + ".validator";
        }
    }
}
