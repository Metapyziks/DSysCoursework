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

    private static Host _sSlaveServer;
    private static Host getSlaveServer()
    {
        if (_sSlaveServer != null && _sSlaveServer.isConnected()) {
            return _sSlaveServer;
        }

        _sSlaveServer = null;
        int minID = Integer.MAX_VALUE;
        for (Host host : getHosts()) {
            int id = host.getIdentifier();
            if (id > _sIdentifier && id <= minID) {
                minID = id;
                _sSlaveServer = host;
            }
        }

        return _sSlaveServer;
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
        for (Student student : _sDatabase) {
            host.insertIntoDatabase(student.serializeToString());
        }
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

    @Override
    public String insertIntoDatabase(String str)
    {
        try {
            Student student = Student.parse(this, str);

            if (student.identifier <= 0) {
                student.identifier = 1;
                for (Student other : _sDatabase) {
                    if (other.identifier >= student.identifier) {
                        student.identifier = other.identifier + 1;
                    }
                }
            }

            _sDatabase.add(student);
            propagate(str);
            return "SUCCESS\n" + student.toString() + "\n";
        } catch (Exception e) {
            return "FAILURE\n";
        }
    }

    @Override
    public String selectFromDatabase(String queryStr)
    {
        Query query;
        try {
            query = Query.parse(queryStr);
        } catch (Exception e) {
            return new QueryResponse(e.getMessage()).toString();
        }

        ArrayList<Student> matches = new ArrayList<Student>();

        for (Student student : _sDatabase) {
            if (query.evaluate(student)) matches.add(student);
        }

        Student[] arr = new Student[matches.size()];
        matches.toArray(arr);
        return "Selected " + arr.length + " item(s):\n" + new QueryResponse(arr).toString();
    }

    @Override
    public String deleteFromDatabase(String queryStr)
    {
        Query query;
        try {
            query = Query.parse(queryStr);
        } catch (Exception e) {
            return new QueryResponse(e.getMessage()).toString();
        }

        ArrayList<Student> matches = new ArrayList<Student>();

        for (Student student : _sDatabase) {
            if (query.evaluate(student)) matches.add(student);
        }

        for (Student student : matches) {
            _sDatabase.remove(student);
        }

        propagate(queryStr);

        Student[] arr = new Student[matches.size()];
        matches.toArray(arr);
        return "Deleted " + arr.length + " item(s):\n" + new QueryResponse(arr).toString();
    }
}
