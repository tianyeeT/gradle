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

package org.gradle.internal.logging.events;

import org.gradle.internal.Either;

public class IntQuestionPromptEvent extends PromptOutputEvent {
    private final int minValue;
    private final int defaultValue;

    public IntQuestionPromptEvent(long timestamp, String prompt, int minValue, int defaultValue) {
        super(timestamp, prompt);
        this.minValue = minValue;
        this.defaultValue = defaultValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Either<Integer, String> convert(String text) {
        if (text.isEmpty()) {
            return Either.left(defaultValue);
        }
        String trimmed = text.trim();
        try {
            int result = Integer.parseInt(trimmed);
            if (result >= minValue) {
                return Either.left(result);
            }
            return Either.right("Please enter an integer value >= " + minValue + " (default: " + defaultValue + "): ");
        } catch (NumberFormatException e) {
            return Either.right("Please enter an integer value (min: " + minValue + ", default: " + defaultValue + "): ");
        }
    }
}
