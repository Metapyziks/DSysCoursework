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
        for (Host host : getHosts()) {
            int id = host.getIdentifier();
            if (id > 0 && id <= minID) {
                minID = id;
                master = host;
            }
        }

        return master;
    }

    public static String joinStringArray(String separator, String[] array, int count)
    {
        if (count == 0) return "";

        StringBuffer buffer = new StringBuffer();
        if (array.length > 0) buffer.append(array[0]);

        for(int i = 1; i < count; ++ i) {
            buffer.append(separator);
            if (i < array.length) buffer.append(array[i]);
        }

        return buffer.toString();
    }

    public static String joinStringArray(String separator, String[] array)
    {
        return joinStringArray(separator, array, array.length);
    }

    public static String[] splitCommand(String command)
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

    private static boolean invokeCommandMethod(Endpoint endpoint, String command, String[] args)
    {
        Method method;
        try {
            method = endpoint.getClass().getMethod("cmd_" + command, Endpoint.class, String[].class);
            if (method.getAnnotation(Command.class) == null) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException e) {
            return false;
        }

        try {
            method.invoke(null, endpoint, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private static boolean invokeCommandMethod(Endpoint endpoint, String command)
    {
        String[] split = splitCommand(command);

        if (split.length == 0) return true;
        
        for (int i = split.length; i > 0; -- i) {
            if (invokeCommandMethod(endpoint, joinStringArray("_", split, i),
                Arrays.copyOfRange(split, i, split.length))) {
                return true;
            }
        }

        return false;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Command {
        String description();
    }

    @Command(description = "prints the name and location of the current master server")
    public static void cmd_get_master(Endpoint endpoint, String[] args)
    {
        log("Finding master...");
        Host master = getMasterServer();
        if (master != null) log("{0}", master);
        else log("No servers online");
    }

    @Command(description = "closes any active connections and stops the application")
    public static void cmd_exit(Endpoint endpoint, String[] args)
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
            if (!invokeCommandMethod(endpoint, line)) {
                log("Unknown command");
            }

            if (_sReadInput) {
                System.out.print("> ");
            } else {
                return;
            }
        }
    }
}
