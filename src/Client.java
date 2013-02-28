import java.rmi.*;
import java.rmi.registry.*;

public class Client
    extends Endpoint
{
    private Client() { }

    public static void main(String[] args)
    {
        try {
            Registry registry = LocateRegistry.getRegistry(ADDRESS, PORT);
            IDatabaseConnection dbcon = (IDatabaseConnection) registry.lookup("DatabaseConnection");

            log(dbcon.testMethod());
        } catch (Exception e) {
            log("An error has occured:");
            e.printStackTrace();
        }
        return;
    }
}
