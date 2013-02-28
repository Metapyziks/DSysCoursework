import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDatabaseConnection extends Remote
{
    String testMethod() throws RemoteException;
}
