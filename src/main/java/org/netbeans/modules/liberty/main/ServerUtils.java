/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 2016 - All Rights Reserved.
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package org.netbeans.modules.liberty.main;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ServerUtils {

    public static final String BATCH_SCRIPT = "server";
    public static final String WLP_USER_DIR = "WLP_USER_DIR";
    public static final String WLP_OUTPUT_DIR = "WLP_OUTPUT_DIR";

    public enum ServerMode {
        RUN("run"), DEBUG("debug");

        private String value = "";

        private ServerMode(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    };

    public static void main(String[] args) {
        String runtimeLocation = "C:\\myLibertyInstallPath\\wlp";
        File userDir = new File(runtimeLocation + "\\usr");
        File serverOutputPath = null;
        String serverName = "defaultServer";
        int debugPort = 7777;

        File javaHome = new File("C:\\myJDKPath\\sdk");

        ServerInfo serverInfo = new ServerInfo(userDir, serverOutputPath, serverName, runtimeLocation, javaHome, debugPort);

        ServerUtils serverUtil = new ServerUtils();
        Process p = serverUtil.startServer(serverInfo, ServerMode.RUN);

        boolean isContinue = true;
        int count = 15;
        while (isContinue && count-- > 0) {
            try {
                if (p.exitValue() <= 0) {
                    System.out.println("Server has stopped from outside: exitValue = " + p.exitValue());
                    isContinue = false;
                }
            } catch (IllegalThreadStateException e1) {
                // Process not exit yet. Continue to wait.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    // TODO handle error.
                    e2.printStackTrace();
                }
            }
        }

        if (isContinue) {
            // Stop the server after the wait time.
            System.out.println("Stopping the server after 10 sec...");
            serverUtil.stopServer(serverInfo);
        }
    }

    /**
     * Start the server in the given mode.
     *
     * @param curServerInfo server information for starting the server.
     * @param mode start mode of the server
     * @return true if launch is successful; otherwise, return false.
     */
    public Process startServer(ServerInfo curServerInfo, ServerMode mode) {

        ProcessBuilder pb = createProcessBuilder(mode.getValue(), curServerInfo);

        if (mode.equals(ServerMode.DEBUG)) {
            Map<String, String> env = pb.environment();
            String vmArgs = env.get("JVM_ARGS");

            int debugPort = curServerInfo.getDebugPort();
            env.put("WLP_DEBUG_ADDRESS", debugPort + "");

            // configure vm arguments
//      if (!useDebugScript) {
            if (vmArgs == null) {
                vmArgs = "";
            } else {
                vmArgs += " ";
            }
            vmArgs += " -Dwas.debug.mode=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:" + debugPort;
//      }

            if (vmArgs != null) {
                env.put("JVM_ARGS", vmArgs);
            }
        }
        Process p = null;
        try {
            System.out.println("Starting the server:");
            for (String curCommand : pb.command()) {
                System.out.println("\t" + curCommand);
            }
            p = pb.start();
            // TODO: add code to hook process to NetBeans
        } catch (IOException e) {
            // TODO: add handle error code.
            e.printStackTrace();
        }

        if (mode.equals(ServerMode.DEBUG)) {
            // TODO: Add code to connect to debugger.
        }

        return p;
    }

    /**
     * Stop the server.
     *
     * @param curServerInfo server information for starting the server.
     * @return true if launch is successful; otherwise, return false.
     */
    public Process stopServer(ServerInfo curServerInfo) {

        ProcessBuilder pb = createProcessBuilder("stop", curServerInfo);

        Process p = null;
        try {
            System.out.println("Stopping the server:");
            for (String curCommand : pb.command()) {
                System.out.println("\t" + curCommand);
            }
            p = pb.start();
            // TODO: add code to hook process to NetBean
        } catch (IOException e) {
            // TODO: add handle error code.
            e.printStackTrace();
        }

        return p;
    }

    /**
     * Create a process builder for launching "wlp/bin/server [option] serverName" with the given arguments.
     *
     * @param option
     * @param server a server
     * @param command command arguments
     * @return the process builder, ready for launch
     */
    public ProcessBuilder createProcessBuilder(String option, ServerInfo curServerInfo, String... command) {
        File workDir = curServerInfo.getServerOutputPath();
        if (!workDir.exists()) {
            workDir = curServerInfo.getUserDir();
        }

        String[] command2 = new String[command.length + 3];
        command2[0] = BATCH_SCRIPT;
        command2[1] = option;
        command2[2] = curServerInfo.getServerName();
        System.arraycopy(command, 0, command2, 3, command.length);
        return createProcessBuilder(curServerInfo.getRuntimeLocation(), curServerInfo.getJavaHome(), curServerInfo.getServerOutputPath(), curServerInfo.getUserDir(), workDir, null, command2);
    }

    /**
     * Create a process builder for launching WLP with the given arguments.
     *
     * @param userDir a user directory, may be null
     * @param workDir a working directory
     * @param command the command arguments
     * @return the process builder, ready for launch
     */
    public ProcessBuilder createProcessBuilder(String runtimeLocation, File javaHome, File serverOutputPath, File userDir, File workDir, String extraJvmArgs, String... command) {
        String batch = command[0];
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            batch += ".bat";
        }
        command[0] = runtimeLocation + File.separator + "bin" + File.separator + batch;

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(workDir);

        Map<String, String> env = builder.environment();
        if (userDir != null) {
            env.put(WLP_USER_DIR, userDir.getPath());
            env.put(WLP_OUTPUT_DIR, serverOutputPath.getAbsolutePath());
        }

        env.put("JAVA_HOME", javaHome.getAbsolutePath());
        String jvmArgs = extraJvmArgs;
        if (jvmArgs != null) {
            env.put("JVM_ARGS", jvmArgs);
        }
        env.put("EXIT_ALL", "1");
        builder.command(command);
        return builder;
    }

}
