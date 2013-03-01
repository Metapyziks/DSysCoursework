import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.lang.reflect.*;

public class Server
    extends Endpoint
    implements IDatabaseConnection
{
    private static Registry _sRegistry;
    private static Server _sLocalServer;

    private static int _sIdentifier;
    private static String _sTestPhrase;

    private static ArrayList<Student> _sDatabase;

    private static void connect(String address, int port)
        throws RemoteException, AlreadyBoundException
    {
        try {
            LocateRegistry.createRegistry(port);
            log("Created new RMI registry at port {0}", Integer.toString(port));
        } catch (RemoteException e) {
            log("Detected existing RMI registry");
        }

        _sRegistry = LocateRegistry.getRegistry(address, port);
        log("Connected to RMI registry at {0} {1}", address, Integer.toString(port));

        _sLocalServer = new Server();
        IDatabaseConnection dbcon = (IDatabaseConnection) UnicastRemoteObject.exportObject(_sLocalServer, 0);
        _sRegistry.bind("DatabaseConnection", dbcon);
        log("DatabaseConnection bound to registry");
    }

    private static void disconnect()
        throws RemoteException, NotBoundException
    {
        _sRegistry.unbind("DatabaseConnection");
        UnicastRemoteObject.unexportObject(_sLocalServer, true);
    }

    private static Server getLocalServer()
    {
        return _sLocalServer;
    }

    private static Host getSlaveServer()
    {
        Host next = null;
        int minID = Integer.MAX_VALUE;
        for (Host host : getHosts()) {
            int id = host.getIdentifier();
            if (id > _sIdentifier && id <= minID) {
                minID = id;
                next = host;
            }
        }

        return next;
    }

    private static Host getServer(int identifier)
    {
        for (Host host : getHosts()) {
            int id = host.getIdentifier();
            if (id == identifier) return host;
        }

        return null;
    }

    private static void findIdentifier()
    {
        log("Polling servers to find identifier...");

        int maxID = _sIdentifier = 0;
        for (Host host : getHosts()) {
            try {
                int id = host.getIdentifier();
                if (id > maxID) maxID = id;
            } catch (Exception e) {
                continue;
            }
        }

        _sIdentifier = maxID + 1;
        log("Server identifier: {0}", _sIdentifier);
    }

    private static void synchronize()
    {
        Host master = getMasterServer();
        if (master != null && master.getIdentifier() != _sIdentifier) {
            log("Synchronizing with master server...");
            master.requestSync(_sIdentifier);
        }
    }

    private static void propagate(Object... args)
    {
        Host slave = getSlaveServer();

        if (slave != null) {
            try {
                 slave.attemptInvokeRemote(1, args);
            } catch (Exception e) { }
        }
    }

    @Command(description = "prints the identification number of this server")
    public static void cmd_get_identifier(Endpoint endpoint, String[] args)
    {
        log("{0}", _sIdentifier);
    }

    @Command(description = "prints the name and location of the current slave of this server")
    public static void cmd_get_slave(Endpoint endpoint, String[] args)
    {
        log("Finding slave...");
        Host slave = getSlaveServer();
        if (slave != null) log("{0}", slave);
        else log("This server is last in the chain");
    }

    public static void main(String[] args)
    {
        _sTestPhrase = "Hello world!";
        _sDatabase = new ArrayList<Student>();

        initializeDepartments("../departments.txt");
        initializeHosts(args.length > 0 ? args[0] : "../hosts.txt");

        String address = "localhost";
        if (args.length > 1 && args[1].length() > 0) address = args[1];

        int port = 3125;
        if (args.length > 2 && args[2].length() > 0) port = Integer.parseInt(args[2]);

        try {
            connect(address, port);
            findIdentifier();
            synchronize();
            readConsoleInput(getLocalServer());
            disconnect();
        } catch (Exception e) {
            log("An {0} has occurred:", e.toString());
            e.printStackTrace();
        }
    }

    private Server() { }

    @Override
    public int getIdentifier()
    {
        return _sIdentifier;
    }

    @Override
    public String ping()
    {
        return "pong";
    }

    @Override
    public void requestSync(int identifier)
    {
        if (identifier == getIdentifier()) return;

        Host host = getServer(identifier);
        if (host == null) return;

        host.setTestPhrase(getTestPhrase());
    }

    @Override
    public String getTestPhrase()
    {
        return _sTestPhrase;
    }

    @Override
    public void setTestPhrase(String phrase)
    {
        _sTestPhrase = phrase;
        propagate(phrase);
    }

    private class Operator
    {
        public static final int NONE = 0;
        public static final int GREATER_OR_EQUAL = 1;
        public static final int LESS_OR_EQUAL = 2;
        public static final int GREATER_THAN = 3;
        public static final int LESS_THAN = 4;
        public static final int EQUAL = 5;
        public static final int NOT_EQUAL = 6;
    }

    private class QueryCondition
    {
        public boolean not;
        public Field field;
        public int operator;
        public Object constant;

        public QueryCondition()
        {
            not = false;
            field = null;
            operator = Operator.NONE;
            constant = null;
        }

        public boolean evaluate(Student student)
        {
            return true;
        }
    }

    @Override
    public String insertIntoDatabase(String str)
    {
        try {
            Student student = Student.parse(this, str);
            _sDatabase.add(student);
            return "SUCCESS\n" + student.serializeToString() + "\n";
        } catch (Exception e) {
            return "FAILURE\n";
        }
    }

    @Override
    public String queryDatabase(String query)
    {
        String[] split = splitCommand(query);

        final String[] validOperators = new String[] {
            null,  // 0
            ">=", // 1
            "<=", // 2
            ">",  // 3
            "<",  // 4
            "==", // 5
            "!="  // 6
        };

        ArrayList<QueryCondition> conditions = new ArrayList<QueryCondition>();

        int i = 0;
        while (i < split.length)
        {
            QueryCondition condition = new QueryCondition();

            while (split[i].equals("not") && i < split.length) {
                condition.not = !condition.not;
                ++i;
            }

            if (i >= split.length) {
                return new QueryResponse("expected a field name at argument #{0}", i).serializeToString();
            }

            try {
                condition.field = Student.class.getDeclaredField(split[i]);
            } catch (Exception e) {
                return new QueryResponse("invalid field name \"{0}\" at argument #{1}", split[i], i).serializeToString();
            }

            if (++i >= split.length) {
                return new QueryResponse("expected an operator at argument #{0}", i).serializeToString();
            }

            for(int o = 0; o < validOperators.length; ++ o) {
                if (split[i].equals(validOperators[o])) {
                    condition.operator = o;
                    break;
                }
            }

            if (condition.operator == Operator.NONE) {
                return new QueryResponse("invalid operator \"{0}\" at argument #{1}", split[i], i).serializeToString();
            }

            if (++i >= split.length) {
                return new QueryResponse("expected a constant value at argument #{0}", i).serializeToString();
            }

            try {
                condition.constant = Integer.parseInt(split[i]);
            } catch (Exception e) {
                condition.constant = split[i];
            }

            conditions.add(condition);

            ++i;
        }

        ArrayList<Student> matches = new ArrayList<Student>();

        for (Student student : _sDatabase) {
            boolean match = true;
            for (QueryCondition condition : conditions) {
                if (!condition.evaluate(student)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                matches.add(student);
            }
        }

        Student[] arr = new Student[matches.size()];
        matches.toArray(arr);
        return new QueryResponse(arr).serializeToString();
    }
}
