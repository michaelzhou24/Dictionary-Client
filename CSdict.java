
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

                //System.out.println("The command is: " + command);
                len = arguments.length;
                //System.out.println("The arguments are: ");
                //for (int i = 0; i < len; i++) {
                //    System.out.println("    " + arguments[i]);
                //}
                if (command.startsWith("#") || command.equals("")) {
                    arguments = null;
                    len = 0;
                    command = "";
                    cmdString = new byte[MAX_LEN];
                    inputs = null;
                    continue;
                }
                switch (command) {
                    case "open": {
                        // >open SERVER PORT
                        // check command validity
                        if (isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 2) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }

                        int portNumber;
                        // Check if valid port number (Integer)
                        try {
                            portNumber = Integer.parseInt(arguments[1]);
                            if (portNumber < 0)
                                throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            System.out.println("902 Invalid argument.");
                            break;
                        }
                        String hostName = arguments[0];
                        // Not sure how to check if hostname is proper, hostname could be IPv4 or hostname

                        //System.out.println("open "+hostName+" "+portNumber);
                        dictServer = "*";
                        try {
                            // Open the socket
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(hostName, portNumber), 10000); // timeout after 10s
                            out = new PrintWriter(socket.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            isOpen = true;
                        } catch (Exception e) {
                            System.err.println("920 Control connection to "+hostName+" on port "+portNumber+" failed to open.");
                        }
                        break;
                    }
                    case "dict": {
                        // >dict
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 0) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }
                        try {
                            stdIn = new BufferedReader(new InputStreamReader(System.in));
                            String cmd = "SHOW DB";
                            String fromServer;
                            if (debugOn) 
                                System.out.println("--> "+cmd);
                            out.println(cmd);
                            // Process output - filter out status codes and errors
                            while ((fromServer = in.readLine()) != null) {
                                if (fromServer.startsWith("250 ok")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    break;
                                }
                                if (fromServer.startsWith("554")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("999 Processing error. No databases present.");
                                }
                                if ((fromServer.startsWith("220") || fromServer.startsWith("110")) && !debugOn) // Suppress status message
                                    continue;
                                if ((fromServer.startsWith("220") || fromServer.startsWith("110")) && debugOn) {
                                    // Suppress status message
                                    System.out.println("<-- "+fromServer);
                                    continue;
                                }
                                System.out.println(fromServer);
                            }
                        } catch (Exception e) {
                            //System.err.println("NullPointerException thrown! Check connection to server..");
                            System.err.println("925 Control connection I/O error, closing control connection.");

                            socket.close();
                            socket = null;
                            in = null;
                            out = null;
                            isOpen = false;
                        }
                        break;
                    }
                    case "set": {
                        // >set DICTIONARY
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }
                        dictServer = arguments[0];
                        // set the variable to the argument 
                        break;
                    }
                    case "define": {
                        // >define WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }
                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        String cmd = "DEFINE " + dictServer + " "+ arguments[0];
                        String fromServer;
                        if (debugOn)
                            System.out.println("--> "+cmd);
                        out.println(cmd);

                        boolean nodef = false;
                        try {
                            // process output
                            while ((fromServer = in.readLine()) != null) {
                                // format @ easton "Easton's ...."
                                if (fromServer.startsWith("550 invalid database")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("999 Processing error. Specified dictionary server not found.");
                                    break;
                                }
                                if (fromServer.startsWith("151")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("@" +
                                            fromServer.substring(fromServer.indexOf(" ", fromServer.indexOf(" ") + 1), fromServer.length()));
                                    continue;
                                }
                                if ((fromServer.startsWith("220") || fromServer.startsWith("150")) && !debugOn) // Suppress status message
                                    continue;
                                if ((fromServer.startsWith("220") || fromServer.startsWith("150")) && debugOn) {
                                    System.out.println("<-- "+fromServer);
                                    continue;
                                }


                                if (fromServer.startsWith("250 ok")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    break;
                                }

                                if (fromServer.startsWith("552 no match") && !nodef) {
                                    System.out.println("*** No definition found! ***");
                                    //cmd = "MATCH * exact "+ arguments[0];
                                    nodef = true;
                                    //out.println(cmd);
                                    if (debugOn) {
                                        System.out.println("<-- "+fromServer);
                                    }
                                    break;
                                }
                                System.out.println(fromServer);
                            }
                        // if no matches found, search all databases
                        if (nodef) {
                            cmd = "MATCH * exact "+ arguments[0];
                            if (debugOn)
                                System.out.println("--> "+cmd);
                            out.println(cmd);
                            while ((fromServer = in.readLine()) != null) {
                                if (fromServer.startsWith("151")) {
                                    System.out.println("@" +
                                            fromServer.substring(fromServer.indexOf(" ", fromServer.indexOf(" ") + 1), fromServer.length()));
                                    continue;
                                }
                                if (fromServer.startsWith("250 ok")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    break;
                                }

                                if ((fromServer.equals(".") || fromServer.startsWith("152") || fromServer.startsWith("220")) && !debugOn)  // Suppress status message
                                    continue;
                                if ((fromServer.equals(".") || fromServer.startsWith("152") || fromServer.startsWith("220")) && debugOn) {
                                    // Suppress status message
                                    System.out.println("<-- "+fromServer);
                                    continue;
                                }

                                if (fromServer.startsWith("552")) {
                                    System.out.println("*** No matches found! ***");
                                    if (debugOn) {
                                        System.out.println("<-- "+fromServer);
                                    }
                                    break;
                                }
                                System.out.println(fromServer);
                            }
                        }
                        } catch (Exception e) {
                            System.err.println("925 Control connection I/O error, closing control connection.");
                            if (socket != null)
                                socket.close();
                            socket = null;
                            in = null;
                            out = null;
                            isOpen = false;
                        }
                        break;
                    }
                    case "match": {
                        // >match WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }
                        String cmd = "MATCH " + dictServer+" exact "+ arguments[0];
                        String fromServer;
                        if (debugOn)
                            System.out.println("--> "+cmd);
                        out.println(cmd);
                        // Same output processing as prefixmatch
                        try {
                            while ((fromServer = in.readLine()) != null) {
                                if (fromServer.startsWith("151")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("@" +
                                            fromServer.substring(fromServer.indexOf(" ", fromServer.indexOf(" ") + 1), fromServer.length()));
                                    continue;
                                }
                                if (fromServer.startsWith("250 ok")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    break;
                                }
                                if ((fromServer.startsWith("152") || fromServer.startsWith("220")) && !debugOn) // Suppress status message
                                    continue;
                                if ((fromServer.startsWith("152") || fromServer.startsWith("220")) && debugOn) {
                                    // Suppress status message
                                    if (debugOn)
                                        System.out.println("<-- " + fromServer);
                                    continue;
                                }
                                if (fromServer.startsWith("552")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("*****No matching word(s) found*****");
                                    break;
                                }

                                System.out.println(fromServer);
                            }
                        } catch (Exception e) {
                            System.err.println("925 Control connection I/O error, closing control connection.");

                            socket.close();
                            socket = null;
                            in = null;
                            out = null;
                            isOpen = false;
                        }
                        break;
                    }
                    case "prefixmatch": {
                        // prefixmatch WORD
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 1) {
                            System.out.println("901 Incorrect number of arguments");
                            break;
                        }
                        String cmd = "MATCH " + dictServer+" prefix "+ arguments[0];
                        String fromServer;
                        if (debugOn)
                            System.out.println("--> "+cmd);
                        out.println(cmd);
                        try {
                            while ((fromServer = in.readLine()) != null) {
                                if (fromServer.startsWith("151")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("@" +
                                        fromServer.substring(fromServer.indexOf(" ", fromServer.indexOf(" ") + 1), fromServer.length()));
                                    continue;
                                }
                                if (fromServer.startsWith("250 ok")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    break;
                                }
                                if ((fromServer.startsWith("152") || fromServer.startsWith("220")) && !debugOn) // Suppress status message
                                    continue;
                                if ((fromServer.startsWith("152") || fromServer.startsWith("220")) && debugOn) {
                                    // Suppress status message
                                    if (debugOn)
                                        System.out.println("<-- " + fromServer);
                                    continue;
                                }
                                if (fromServer.startsWith("552")) {
                                    if (debugOn)
                                        System.out.println("<-- "+fromServer);
                                    System.out.println("*****No matching word(s) found*****");
                                    break;
                                }
                                System.out.println(fromServer);
                            }
                        } catch (Exception e) {
                            System.err.println("925 Control connection I/O error, closing control connection.");
                            socket.close();
                            socket = null;
                            in = null;
                            out = null;
                            isOpen = false;
                        }
                        break;
                    }
                    case "close": {
                        if (!isOpen) {
                            System.out.println("903 Supplied command not expected at this time.");
                            break;
                        }
                        if (len != 0)
                            System.out.println("901 Incorrect number of arguments");

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
                        if (socket != null)
                            socket.close();
                        System.exit(0);
                        break;
                    }
                    default:
                        System.out.println("900 Invalid Command");
                }
                //System.out.println("Done.");
                // Reset input array
                arguments = null;
                len = 0;
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


