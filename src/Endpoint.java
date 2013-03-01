import java.text.MessageFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
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

    private static void invokeCommandMethod(Endpoint endpoint, String command)
    {
        Method method;

        try {
            method = endpoint.getClass().getMethod(command, String[].class);
            if (method.getAnnotation(Command.class) == null) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException e) {
            log("Unrecognised command");
            return;
        }

        try {
            method.invoke(null, (Object) new String[] { command });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Command
    public static void get(String[] args)
    {
        log("Finding master...");
        Host master = getMasterServer();
        log("Current master: {0}", master);
    }

    @Command
    public static void exit(String[] args)
    {
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
