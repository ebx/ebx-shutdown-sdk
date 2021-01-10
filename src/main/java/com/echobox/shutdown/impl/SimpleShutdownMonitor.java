/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.echobox.shutdown.impl;

import com.echobox.shutdown.ShutdownMonitor;

/**
 * A wrapper class that can be used to signal request for shutdown of long running
 * operations
 * 
 * @author MarcF 
 */
public class SimpleShutdownMonitor implements ShutdownMonitor {
  
  /**
   * The flag that represents if a shutdown has been requested
   */
  protected volatile boolean shutdownRequested;

  /**
   * Initialise a new shutdown monitor, where the shutdown request is false
   */
  public SimpleShutdownMonitor() {
    this(false);
  }

  /**
   * Initialise a new shutdown monitor using the provided boolean. This can be used to attached
   * this shutdown monitor to an existing boolean
   *
   * @param initialBoolean the initial boolean
   */
  public SimpleShutdownMonitor(boolean initialBoolean) {
    this.shutdownRequested = initialBoolean;
  }

  /**
   * Update the shutdown requested flag to the new value
   * @param newValue the new value
   */
  @Override
  public void setShutdownRequested(boolean newValue) {
    shutdownRequested = newValue;

    //Ensure any wait's on this object are notified
    if (shutdownRequested) {
      synchronized (this) {
        this.notifyAll();
      }
    }
  }

  /**
   * Gets shutdown requested.
   *
   * @return true if a shutdown has been requested
   */
  @Override
  public boolean isShutdownRequested() {
    return shutdownRequested;
  }
  
}
