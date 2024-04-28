/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.plugin.software.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Objects;

/**
 * Represents a resolved software type implementation.  Used by declarative DSL to understand which model types should be exposed for
 * which software types.
 */
public class DefaultSoftwareTypeImplementation implements SoftwareTypeImplementation {
    private final String softwareType;
    private final Class<?> modelPublicType;
    private final Class<? extends Plugin<?>> pluginClass;

    public DefaultSoftwareTypeImplementation(String softwareType, Class<?> modelPublicType, Class<? extends Plugin<Project>> pluginClass) {
        this.softwareType = softwareType;
        this.modelPublicType = modelPublicType;
        this.pluginClass = pluginClass;
    }

    @Override
    public String getSoftwareType() {
        return softwareType;
    }

    @Override
    public Class<?> getModelPublicType() {
        return modelPublicType;
    }

    @Override
    public Class<? extends Plugin<?>> getPluginClass() {
        return pluginClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultSoftwareTypeImplementation that = (DefaultSoftwareTypeImplementation) o;
        return Objects.equals(softwareType, that.softwareType) && Objects.equals(modelPublicType, that.modelPublicType) && Objects.equals(pluginClass, that.pluginClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(softwareType, modelPublicType, pluginClass);
    }
}
