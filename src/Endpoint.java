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
    private static Department[] _sDepartments;

    public static void initializeHosts(String filePath)
    {
        log("Reading hosts file \"{0}\"", filePath);
        _sHosts = Host.readFromFile(filePath);
        log("Found {0} host definitions", _sHosts.length);
    }

    public static void initializeDepartments(String filePath)
    {
        log("Reading departments file \"{0}\"", filePath);
        _sDepartments = Department.readFromFile(filePath);
        log("Found {0} department definitions", _sDepartments.length);
    }

    public static Host[] getHosts()
    {
        return _sHosts;
    }

    public static Department[] getDepartments()
    {
        return _sDepartments;
    }

    private static Host _sMasterServer;
    public static Host getMasterServer()
    {
        if (_sMasterServer != null && _sMasterServer.isConnected()) {
            return _sMasterServer;
        }

        _sMasterServer = null;
        int minID = Integer.MAX_VALUE;
        for (Host host : getHosts()) {
            int id = host.getIdentifier();
            if (id > 0 && id <= minID) {
                minID = id;
                _sMasterServer = host;
            }
        }

        return _sMasterServer;
    }

    public static Department getDepartment(int identifier)
    {
        for (int i = 0; i < _sDepartments.length; ++i) {
            if (_sDepartments[i].identifier == identifier) {
                return _sDepartments[i];
            }
        }

        return null;
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

    private static Method getCommandMethod(Endpoint endpoint, String command)
    {
        Method method;
        try {
            method = endpoint.getClass().getMethod("cmd_" + command, Endpoint.class, String[].class);
            if (method.getAnnotation(Command.class) == null) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException e) {
            return null;
        }

        return method;
    }

    private static ArrayList<Method> getAllCommandMethods(Class clss)
    {
        ArrayList<Method> list = new ArrayList<Method>();
        for (Method method : clss.getDeclaredMethods()) {
            if (method.getAnnotation(Command.class) != null && method.getName().startsWith("cmd_")) {
                list.add(method);
            }
        }

        if (clss.getSuperclass() != Object.class) {
            for (Method method : getAllCommandMethods(clss.getSuperclass())) {
                list.add(method);
            }
        }

        return list;
    }

    private static boolean invokeCommandMethod(Endpoint endpoint, String command, String[] args)
    {
        Method method = getCommandMethod(endpoint, command);

        if (method == null) return false;

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
        String description() default "no description";
        int minArgs() default 2;
    }

    @Command(description = "prints the name and location of the current master server")
    public static void cmd_get_master(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) log("{0}", master);
        else log("No servers online");
    }

    @Command(description = "gets the name of a department specified by identification number",
        minArgs = 1)
    public static void cmd_get_department(Endpoint endpoint, String[] args)
    {
        try {
            Department department = getDepartment(Integer.parseInt(args[0]));
            if (department != null) {
                log(department.name);
            } else {
                log("Department with identifier {0} not found", args[0]);
            }
        } catch (Exception e) {
            log("Invalid department identifier");
        }
    }

    @Command(description = "lists all known department names by identifier")
    public static void cmd_get_department_all(Endpoint endpoint, String[] args)
    {
        int maxWidth = 0;
        for (Department dept : _sDepartments) {
            int width = (int) Math.log10(Math.max(1, dept.identifier)) + 1;
            if (width > maxWidth) maxWidth = width;
        }

        for (Department dept : _sDepartments) {
            int width = (int) Math.log10(Math.max(1, dept.identifier)) + 1;
            String spaces = "";
            while (width + spaces.length() < maxWidth) spaces += " ";
            log("{0}{1} : {2}", spaces, dept.identifier, dept.name);
        }
    }

    @Command(description = "looks like you already know how to use this command!")
    public static void cmd_help(Endpoint endpoint, String[] args)
    {
        if (args.length == 0) {
            for (Method method : getAllCommandMethods(endpoint.getClass())) {
                Command annotation = (Command) method.getAnnotation(Command.class);
                log("----\n- {0}\n----\n{1}\n", method.getName().substring(4).replace("_", " "), annotation.description());
            }
        } else {
            Method method = getCommandMethod(endpoint, joinStringArray("_", args));
            if (method != null) {
                Command annotation = (Command) method.getAnnotation(Command.class);
                log(annotation.description());
            } else {
                log("Command not recognised");
            }
        }
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
                log("Command not recognised");
            }

            if (_sReadInput) {
                System.out.print("> ");
            } else {
                return;
            }
        }
    }
}
