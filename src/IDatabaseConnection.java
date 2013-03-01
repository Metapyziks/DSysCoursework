import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDatabaseConnection extends Remote
{
    int getIdentifier() throws RemoteException;
    String testMethod() throws RemoteException;
}
