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

package org.gradle.launcher.daemon.server.clientinput;

import org.gradle.api.internal.tasks.userinput.UserInputReader;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.IoActions;
import org.gradle.launcher.daemon.protocol.ForwardInput;
import org.gradle.launcher.daemon.protocol.UserResponse;
import org.gradle.launcher.daemon.server.api.StdinHandler;

import java.io.InputStream;
import java.util.function.Function;

/**
 * Forwards user input received from the client to this process' System.in and {@link UserInputReader}.
 */
public class ClientInputForwarder {
    private static final Logger LOGGER = Logging.getLogger(ClientInputForwarder.class);
    private final UserInputReader inputReader;

    public ClientInputForwarder(UserInputReader inputReader) {
        this.inputReader = inputReader;
    }

    public <T> T forwardInput(Function<StdinHandler, T> action) {
        final StdInStream stdInStream = new StdInStream();
        inputReader.startInput();

        StdinHandler stdinHandler = new StdinHandler() {
            @Override
            public void onInput(ForwardInput input) {
                LOGGER.debug("Writing forwarded input on this process' stdin.");
                stdInStream.received(input.getBytes());
            }

            @Override
            public void onUserResponse(UserResponse input) {
                inputReader.putInput(new UserInputReader.TextResponse(input.getResponse()));
            }

            @Override
            public void onEndOfInput() {
                LOGGER.debug("Closing this process' stdin at end of input.");
                try {
                    stdInStream.close();
                    inputReader.putInput(UserInputReader.END_OF_INPUT);
                } finally {
                    LOGGER.debug("This process will no longer process any forwarded input.");
                }
            }
        };

        InputStream previousStdin = System.in;
        try {
            System.setIn(stdInStream);
            return action.apply(stdinHandler);
        } finally {
            System.setIn(previousStdin);
            IoActions.closeQuietly(stdInStream);
        }
    }
}
