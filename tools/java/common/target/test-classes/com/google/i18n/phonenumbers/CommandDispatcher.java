/*
 *  Copyright (C) 2011 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

/**
 * This class is designed to execute a requested command among a set of provided commands.
 * The dispatching is performed according to the requested command name, which is provided as the
 * first string of the 'args' array. The 'args' array also contains the command arguments available
 * from position 1 to end. The verification of the arguments' consistency is under the
 * responsibility of the command since the dispatcher can't be aware of its underlying goals.
 *
 * @see Command
 * @author Philippe Liard
 */
public class CommandDispatcher {
  // Command line arguments passed to the command which will be executed. Note that the first one is
  // the name of the command.
  private final String[] args;
  // Supported commands by this dispatcher.
  private final Command[] commands;

  public CommandDispatcher(String[] args, Command[] commands) {
    this.args = args;
    this.commands = commands;
  }

  /**
   * Executes the command named `args[0]` if any. If the requested command (in args[0]) is not
   * supported, display a help message.
   *
   * <p> Note that the command name comparison is case sensitive.
   */
  public boolean start() {
    if (args.length != 0) {
      String requestedCommand = args[0];

      for (Command command : commands) {
        if (command.getCommandName().equals(requestedCommand)) {
          command.setArgs(args);
          return command.start();
        }
      }
    }
    displayUsage();
    return false;
  }

  /**
   * Displays a message containing the list of the supported commands by this dispatcher.
   */
  private void displayUsage() {
    StringBuilder msg = new StringBuilder("Usage: java -jar /path/to/jar [ ");
    int i = 0;

    for (Command command : commands) {
      msg.append(command.getCommandName());
      if (i++ != commands.length - 1) {
        msg.append(" | ");
      }
    }
    msg.append(" ] args");
    System.err.println(msg.toString());
  }
}
