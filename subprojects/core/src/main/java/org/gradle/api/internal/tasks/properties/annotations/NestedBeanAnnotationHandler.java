/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.properties.annotations;

import org.gradle.api.Task;
import org.gradle.api.internal.tasks.PropertySpecFactory;
import org.gradle.api.internal.tasks.TaskValidationContext;
import org.gradle.api.internal.tasks.ValidatingValue;
import org.gradle.api.internal.tasks.ValidationAction;
import org.gradle.api.internal.tasks.properties.BeanPropertyContext;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.reflect.PropertyMetadata;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

import static org.gradle.api.internal.tasks.properties.annotations.InputPropertyAnnotationHandlerUtils.isOptional;

public class NestedBeanAnnotationHandler implements PropertyAnnotationHandler {

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Nested.class;
    }

    @Override
    public boolean shouldVisit(PropertyVisitor visitor) {
        return !visitor.visitOutputFilePropertiesOnly();
    }

    @Override
    public void visitPropertyValue(String propertyName, ValidatingValue value, PropertyMetadata propertyMetadata, PropertyVisitor visitor, PropertySpecFactory specFactory, BeanPropertyContext context) {
        Object nested;
        try {
            nested = unpackProvider(value.call());
        } catch (Exception e) {
            visitor.visitInputProperty(specFactory.createInputPropertySpec(propertyName, new InvalidValue(e)));
            return;
        }
        if (nested != null) {
            context.addNested(propertyName, nested);
        } else if (!isOptional(propertyMetadata)) {
            visitor.visitInputProperty(specFactory.createInputPropertySpec(propertyName, new AbsentValue()));
        }
    }

    @Nullable
    private static Object unpackProvider(@Nullable Object value) {
        // Only unpack one level of Providers, since Provider<Provider<>> is not supported - we don't need two levels of lazyness.
        if (value instanceof Provider) {
            return ((Provider<?>) value).get();
        }
        return value;
    }

    private static class InvalidValue implements ValidatingValue {
        private final Exception exception;

        public InvalidValue(Exception exception) {
            this.exception = exception;
        }

        @Nullable
        @Override
        public Object call() {
            return null;
        }

        @Override
        public void attachProducer(Task producer) {
            // Ignore
        }

        @Override
        public void maybeFinalizeValue() {
            // Ignore
        }

        @Override
        public void validate(String propertyName, boolean optional, ValidationAction valueValidator, TaskValidationContext context) {
            throw UncheckedException.throwAsUncheckedException(exception);
        }
    }

    private static class AbsentValue implements ValidatingValue {
        @Nullable
        @Override
        public Object call() {
            return null;
        }

        @Override
        public void attachProducer(Task producer) {
            // Ignore
        }

        @Override
        public void maybeFinalizeValue() {
            // Ignore
        }

        @Override
        public void validate(String propertyName, boolean optional, ValidationAction valueValidator, TaskValidationContext context) {
            context.recordValidationMessage(String.format("No value has been specified for property '%s'.", propertyName));
        }

    }
}
