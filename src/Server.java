import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Server
    extends Endpoint
    implements IDatabaseConnection
{
    private static Host[] _sHosts;
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

        Registry registry = LocateRegistry.getRegistry(address, port);
        log("Connected to RMI registry at {0} {1}", address, Integer.toString(port));

        Server server = new Server();
        IDatabaseConnection dbcon = (IDatabaseConnection) UnicastRemoteObject.exportObject(server, 0);
        registry.bind("DatabaseConnection", dbcon);
        log("DatabaseConnection bound to registry");
    }

    private static void findIdentifier()
    {
        log("Polling servers to find identifier...");

        int maxID = _sIdentifier = 0;
        for (Host host : _sHosts) {
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

    private static Host getMasterServer()
    {
        Host master = null;
        int minID = _sIdentifier;
        for (Host host : _sHosts) {
            int id = host.getIdentifier();
            if (id > 0 && id <= minID) {
                minID = id;
                master = host;
            }
        }

        return master;
    }

    public static void main(String[] args)
    {
        String hostsFilePath = args.length > 0 ? args[0] : "../hosts.txt";

        log("Reading hosts file \"{0}\"", hostsFilePath);
        _sHosts = Host.readFromFile(hostsFilePath);
        log("Found {0} host definitions", _sHosts.length);

        String address = "localhost";
        if (args.length > 1 && args[1].length() > 0) address = args[1];

        int port = 3125;
        if (args.length > 2 && args[2].length() > 0) port = Integer.parseInt(args[2]);

        try {
            connect(address, port);
            findIdentifier();
            
        } catch (Exception e) {
            log("An {0} has occurred:", e.toString());
            e.printStackTrace();
        }

        String line;
        while ((line = readLine()) != null) {
            if (line.equals("get master")) {
                log("Finding master...");
                Host master = getMasterServer();
                if (master.getIdentifier() == _sIdentifier) {
                    log("This server is the current master");
                } else {
                    log("Current master: {0}", master);
                }
            }
        }
    }

    private Server() { }

    @Override
    public int getIdentifier()
    {
        return _sIdentifier;
    }
}
