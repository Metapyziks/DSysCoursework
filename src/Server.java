import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Server
    extends Endpoint
    implements IDatabaseConnection
{
    private static Registry _sRegistry;
    private static Server _sLocalServer;

    private static int _sIdentifier;
    private static String _sTestPhrase;

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
        log("Sync requested by {0}", identifier);

        if (identifier == getIdentifier()) return;

        Host host = getServer(identifier);
        if (host == null) return;

        host.setTestPhrase(getTestPhrase());
    }

    @Override
    public void setTestPhrase(String phrase)
    {
        _sTestPhrase = phrase;
        log("Test phrase is now \"{0}\"", phrase);

        propagate(phrase);
    }

    @Override
    public String getTestPhrase()
    {
        return _sTestPhrase;
    }
}
