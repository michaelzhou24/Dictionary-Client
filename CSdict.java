
// You can use this file as a starting point for your dictionary client
// The file contains the code for command line parsing and it also
// illustrates how to read and partially parse the input typed by the user.
// Although your main class has to be in this file, there is no requirement that you
// use this template or hav all or your classes in this file.

import java.lang.System;
import java.io.*;
import java.net.*;
import java.util.*;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The only argument the program takes is
// -d which turns on debugging output.
//


public class CSdict {
    static final int MAX_LEN = 255;
    static Boolean debugOn = false;

    private static final int PERMITTED_ARGUMENT_COUNT = 1;
    private static String command;
    private static String[] arguments;

    public static void main(String [] args) {
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null, stdIn = null;
        String dictServer = "*";
        boolean isOpen = false;

        byte cmdString[] = new byte[MAX_LEN];
        int len;
        // Verify command line arguments
        Socket client;
        if (args.length == PERMITTED_ARGUMENT_COUNT) {
            debugOn = args[0].equals("-d");
            if (debugOn) {
                System.out.println("Debugging output enabled");
            } else {
                System.out.println("997 Invalid command line option - Only -d is allowed");
                return;
            }
        } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
            System.out.println("996 Too many command line options - Only -d is allowed");
            return;
        }


        // Example code to read command line input and extract arguments.
        while (true) {
            try {
                System.out.print("csdict> ");
                System.in.read(cmdString);
                // Convert the command string to ASII
                String inputString = new String(cmdString, "ASCII");

                // Split the string into words
                String[] inputs = inputString.trim().split("( |\t)+");
                // Set the command
                command = inputs[0].toLowerCase().trim();
                // Remainder of the inputs is the arguments.
                arguments = Arrays.copyOfRange(inputs, 1, inputs.length);

                System.out.println("The command is: " + command);
                len = arguments.length;
                System.out.println("The arguments are: ");
                for (int i = 0; i < len; i++) {
                    System.out.println("    " + arguments[i]);
                }
                switch (command) {
                    case "open": {
                        // >open SERVER PORT
                        if (len != 2) {
                            System.out.println("901 Incorrect number of arguments");
                            // break; 
                        }
                        if (socket.isConnected() || isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
//                        String hostName = arguments[0];
//                        int portNumber = Integer.parseInt(arguments[1]);
                        String hostName = "test.dict.org";
                        int portNumber = 2628;
                        System.out.println("open "+hostName+" "+portNumber);
                        dictServer = "*";
                        try {
                            socket = new Socket(hostName, portNumber);
                            out = new PrintWriter(socket.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        } catch (UnknownHostException e) {
                            System.err.println("Don't know about host");
                            System.exit(1);
                        } catch (IOException e) {
                            System.err.println("Couldn't get I/O for the connection to");
                            System.exit(1);
                        }
                        isOpen = true;
                        break;
                    }
                    case "dict": {
                        // >dict
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 0)
                            System.out.println("901 Incorrect number of arguments");
                        try {
                            stdIn = new BufferedReader(new InputStreamReader(System.in));
                            String cmd = "SHOW DB";
                            String fromServer;
                            out.println(cmd);
                            while ((fromServer = in.readLine()) != null && !fromServer.equals(".")) {
                                if (!fromServer.startsWith("220"))
                                    continue;
                                System.out.println(fromServer);
                            }
                        } catch (Exception e) {
                            System.err.println("NullPointerException thrown! Check connection to server..");
                        }
                        break;
                    }
                    case "set": {
                        // >set DICTIONARY
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1)
                            System.out.println("901 Incorrect number of arguments");
                        dictServer = arguments[0];
                        break;
                    }
                    case "define": {
                        // >define WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1)
                            System.out.println("901 Incorrect number of arguments");
                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        String cmd = "DEFINE " + dictServer + " "+ arguments[0];
                        System.out.println(cmd);
                        String fromServer;
                        out.println(cmd);
                        while ((fromServer = in.readLine()) != null) {
                            if (fromServer.startsWith("250 ok"))
                                break;
                            if (fromServer.startsWith("220")) // Suppress connection message
                                continue;
                            if (fromServer.startsWith("552 no match")) {
                                System.out.println("*** No definition found! ***");
                                break;
                            }
                            System.out.println(fromServer);
                        }
                        break;
                    }
                    case "match": {
                        // >match WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1)
                            System.out.println("901 Incorrect number of arguments");
                        break;
                    }
                    case "prefixmatch": {
                        // prefixmatch WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1)
                            System.out.println("901 Incorrect number of arguments");
                        break;
                    }
                    case "close": {
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 0)
                            System.out.println("901 Incorrect number of arguments");
                        if (socket != null)
                            socket.close();
                        socket = null;
                        in = null;
                        out = null;
                        isOpen = false;
                        break;
                    }
                    case "quit": {
                        if (len != 0)
                            System.out.println("901 Incorrect number of arguments");
                        System.exit(0);
                        break;
                    }
                    default:
                        System.out.println("900 Invalid Command");
                }
                System.out.println("Done.");
                arguments = null;
                command = "";
                cmdString = new byte[MAX_LEN];
                inputs = null;
            } catch (IOException exception) {
                System.err.println("998 Input error while reading commands, terminating.");
                System.exit(-1);
            }
            
        }
    }
}


