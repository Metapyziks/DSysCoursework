import java.text.MessageFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.annotation.*;
import java.lang.reflect.*;

public class Endpoint
{
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Command { }

    private static boolean _sReadInput;

    public static void log(String msg)
    {
        System.out.println(msg);
    }

    public static void log(String format, Object... arguments)
    {
        log(MessageFormat.format(format, arguments));
    }

    private static BufferedReader _sReader;
    public static String readLine()
    {
        if (_sReader == null) {
            _sReader = new BufferedReader(new InputStreamReader(System.in));
        }

        try {
            return _sReader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static Host[] _sHosts;

    public static void initializeHosts(String filePath)
    {
        log("Reading hosts file \"{0}\"", filePath);
        _sHosts = Host.readFromFile(filePath);
        log("Found {0} host definitions", _sHosts.length);
    }

    public static Host[] getHosts()
    {
        return _sHosts;
    }

    public static Host getMasterServer()
    {
        Host master = null;
        int minID = Integer.MAX_VALUE;
        for (Host host : _sHosts) {
            int id = host.getIdentifier();
            if (id > 0 && id <= minID) {
                minID = id;
                master = host;
            }
        }

        return master;
    }

    private static String[] splitCommand(String command)
    {
        ArrayList<String> list = new ArrayList<String>();

        boolean inString = false, escaped = false;
        String current = "";
        for(int i = 0; i < command.length(); ++i) {
            char c = command.charAt(i);
            if (escaped) {
                current += c;
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (c == ' ') {
                if (inString) {
                    current += c;
                } else if (current.length() > 0) {
                    list.add(current);
                    current = "";
                }
            } else {
                current += c;
            }
        }

        if (current.length() > 0) list.add(current);

        String[] arr = new String[list.size()];
        list.toArray(arr);
        return arr;
    }

    private static void invokeCommandMethod(Endpoint endpoint, String command)
    {
        Method method;
        String[] split = splitCommand(command);

        if (split.length == 0) return;

        try {
            method = endpoint.getClass().getMethod(split[0], String[].class);
            if (method.getAnnotation(Command.class) == null) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException e) {
            log("Unrecognised command \"{0}\"", split[0]);
            return;
        }

        try {
            method.invoke(null, (Object) Arrays.copyOfRange(split, 1, split.length));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Command
    public static void get(String[] args)
    {
        if (args.length != 0) {
            if (args[0].equals("master")) {
                log("Finding master...");
                Host master = getMasterServer();
                log("Current master: {0}", master);
                return;
            }
        }

        log("Usage of command \"get\":");
        log("- get master        | get the current master server");
    }

    @Command
    public static void exit(String[] args)
    {
        log("Exiting...");
        _sReadInput = false;
    }

    public static void readConsoleInput(Endpoint endpoint)
    {
        _sReadInput = true;

        System.out.print("> ");
        String line;
        while ((line = readLine()) != null) {
            invokeCommandMethod(endpoint, line);

            if (_sReadInput) {
                System.out.print("> ");
            } else {
                return;
            }
        }
    }
}
