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
 * Abstract class defining a common interface for commands provided by build tools (e.g: commands to
 * generate code or to download source files).
 *
 * <p> Subclass it to create a new command (e.g: code generation step).
 *
 * @author Philippe Liard
 */
public abstract class Command {
  // The arguments provided to this command. The first one is the name of the command.
  private String[] args;

  /**
   * Entry point of the command called by the CommandDispatcher when requested. This method must be
   * implemented by subclasses.
   */
  public abstract boolean start();

  /**
   * The name of the command is used by the CommandDispatcher to execute the requested command. The
   * Dispatcher will pass along all command-line arguments to this command, so args[0] will be
   * always the command name.
   */
  public abstract String getCommandName();

  public String[] getArgs() {
    return args;
  }

  public void setArgs(String[] args) {
    this.args = args;
  }
}
