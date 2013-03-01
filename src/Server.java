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

    public static void main(String[] args)
    {
        initializeHosts(args.length > 0 ? args[0] : "../hosts.txt");

        String address = "localhost";
        if (args.length > 1 && args[1].length() > 0) address = args[1];

        int port = 3125;
        if (args.length > 2 && args[2].length() > 0) port = Integer.parseInt(args[2]);

        try {
            connect(address, port);
            findIdentifier();
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
}
