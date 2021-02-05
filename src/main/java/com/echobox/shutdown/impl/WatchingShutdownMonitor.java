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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Set;

/**
 * A wrapper class that can be used to signal request for shutdown of long running
 * operations. 
 * <P>
 * Can be configured to shutdown on the shutdown monitor when a shutdown file is created in the 
 * application's working directory, the set shutdown method is called or optionally when a 
 * SIGTERM signal is sent.
 *
 * @author eddspencer
 */
public class WatchingShutdownMonitor extends SimpleShutdownMonitor {

  private static final String CLOSING_FILE = "shutdown";

  /**
   * If true will create CLOSING_FILE when a shutdown is requested
   */
  private final boolean createFileOnShutdown;

  private static final Logger logger = LoggerFactory.getLogger(WatchingShutdownMonitor.class);

  /**
   * Initialise a new shutdown monitor using the provided boolean. This can be used to attached
   * this shutdown monitor to an existing boolean
   *
   * @param initialBoolean the initial boolean
   * @param createFileOnShutdown the create file on shutdown
   */
  private WatchingShutdownMonitor(boolean initialBoolean,
      boolean createFileOnShutdown) {
    super(initialBoolean);
    this.createFileOnShutdown = createFileOnShutdown;
  }

  /**
   * Creates a new Builder instance
   *
   * @return the builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Update the shutdown requested flag to the new value
   * @param newValue the new value
   */
  @Override
  public void setShutdownRequested(boolean newValue) {
    super.setShutdownRequested(newValue);
    
    if (shutdownRequested) {
      onShutdownRequested();
    }
  }

  /**
   * Call when shutdown has been requested
   */
  private void onShutdownRequested() {
    if (createFileOnShutdown) {
      try {
        if (!new File(CLOSING_FILE).exists()) {
          try (PrintWriter pw = new PrintWriter(
              new BufferedWriter(new FileWriter(CLOSING_FILE, false)))) {
            pw.println();
          }
        }
      } catch (Exception ex) {
        logger.error("Failed to create shutdown file.");
      }
    }
  }

  /**
   * Register sigterm shutdown hook, must be called from the main thread to ensure shutdown is
   * graceful.
   */
  private void registerSIGTERMShutdownHook() {
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        setShutdownRequested(true);

        logger.info("Set shutdown flag after SIGTERM signal.");

        //Wait for main thread to finish gracefully.
        mainThread.join();
      } catch (InterruptedException e) {
        logger.error("Failed to gracefully wait for main thread shutdown.");
      }
    }));
  }

  /**
   * Listens for shutdown file in a separate thread.
   *
   * @param threadName the thread name
   * @param checkPeriodSecs the check period secs
   */
  private void listenForShutdownFile(String threadName, int checkPeriodSecs) {
    final Thread mainThread = Thread.currentThread();
    final Thread thread = new Thread(new ShutdownWatcher(mainThread, checkPeriodSecs));
    thread.setName(threadName);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Builder for WatchingShutdownMonitor
   *
   * @author eddspencer
   */
  public static class Builder {
    private static final int ADMIN_FILE_CHECK_SECS = 10;

    private boolean createFileWhenShutdownRequested;
    private boolean listenForShutdownFile;
    private int checkPeriodSecs;
    private String threadName;
    private boolean registerShutdownHook;

    /**
     * Will create file when shutdown requested, default is false.
     *
     * @return the builder
     */
    public Builder createFileWhenShutdownRequested() {
      this.createFileWhenShutdownRequested = true;
      return this;
    }

    /**
     * Listen for shutdown file, default is false. If no period is set used 10 seconds.
     *
     * @param threadName the thread name
     * @return the builder
     */
    public Builder listenForShutdownFile(String threadName) {
      return listenForShutdownFile(threadName, ADMIN_FILE_CHECK_SECS);
    }

    /**
     * Listen for shutdown file, default is false.
     *
     * @param threadName the thread name
     * @param checkPeriodSecs the check period secs
     * @return the builder
     */
    public Builder listenForShutdownFile(String threadName, int checkPeriodSecs) {
      this.listenForShutdownFile = true;
      this.checkPeriodSecs = checkPeriodSecs;
      this.threadName = threadName;
      return this;
    }

    /**
     * Register shutdown hook, default is false.
     *
     * @return the builder
     */
    public Builder registerShutdownHook() {
      this.registerShutdownHook = true;
      return this;
    }

    /**
     * Builds the shutdown monitor, optionally starts listening thread and registers shutdown hook.
     *
     * @return the shutdown monitor
     */
    public ShutdownMonitor build() {
      final WatchingShutdownMonitor shutdownMonitor =
          new WatchingShutdownMonitor(false, createFileWhenShutdownRequested);

      if (listenForShutdownFile) {
        shutdownMonitor.listenForShutdownFile(threadName, checkPeriodSecs);
      }
      if (registerShutdownHook) {
        shutdownMonitor.registerSIGTERMShutdownHook();
      }

      return shutdownMonitor;
    }
  }

  /**
   * Inner class to encapsulate runnable to watch for shutdown file.
   *
   * @author eddspencer
   */
  private class ShutdownWatcher implements Runnable {
    private final Thread mainThread;
    private final int checkPeriodSecs;
  
    private ShutdownWatcher(Thread mainThread, int checkPeriodSecs) {
      this.mainThread = mainThread;
      this.checkPeriodSecs = checkPeriodSecs;
    }
    
    @Override
    public void run() {
      while (true) {
        boolean shutdownExists = new File(CLOSING_FILE).exists();
        if (shutdownExists || isShutdownRequested()) {
          setShutdownRequested(true);
          logger.info("Shutdown file detected - initiating graceful shutdown");
          break;
        } else {
          logger.trace("Closing file not seen.");
          try {
            Thread.sleep(checkPeriodSecs * 1000);
          } catch (InterruptedException ex) {
            //Ignore the interrupted exception if it occurs
          }
        }
      }
  
      try {
        // Wait for main thread to finish gracefully.
        mainThread.join();
    
        // Log any other running threads as not expecting this
        final Set<Thread> threads = Thread.getAllStackTraces().keySet();
        threads.stream()
            .filter(Thread::isAlive)
            .filter(thread -> !thread.isDaemon())
            .filter(thread -> !Thread.currentThread().getName().equals(thread.getName()))
            .forEach(thread -> {
              logger.warn("Shutting down with still running thread '{}'", thread.getName());
            });
      } catch (InterruptedException ex) {
        logger.error("Interrupted waiting for main thread", ex);
      }
      
      logger.info("Finishing close check thread, exiting");
      
      System.exit(0);
    }
  }
}
