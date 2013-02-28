import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Server
    extends Endpoint
    implements IDatabaseConnection
{
    private Server() { }

    @Override
    public String testMethod()
    {
        return "Hello world!";
    }

    public static void main(String[] args)
    {
        try {
            LocateRegistry.createRegistry(PORT);
            log("Created new RMI registry at port {0}", Integer.toString(PORT));
        } catch (RemoteException e) {
            log("Detected existing RMI registry");
        }

        try {
            Registry registry = LocateRegistry.getRegistry(ADDRESS, PORT);
            log("Connected to RMI registry at {0} {1}", ADDRESS, Integer.toString(PORT));

            Server server = new Server();
            IDatabaseConnection dbcon = (IDatabaseConnection) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("DatabaseConnection", dbcon);
            log("DatabaseConnection bound to registry");

        } catch (Exception e) {
            log("An error has occured:");
            e.printStackTrace();
        }
        return;
    }
}
