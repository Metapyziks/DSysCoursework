import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDatabaseConnection extends Remote
{
    int getIdentifier() throws RemoteException;
    String ping() throws RemoteException;

    void requestSync(int identifier) throws RemoteException;

    void setTestPhrase(String phrase) throws RemoteException;
    String getTestPhrase() throws RemoteException;

    String insertIntoDatabase(String student) throws RemoteException;
    String selectFromDatabase(String queryStr) throws RemoteException;
    String deleteFromDatabase(String queryStr) throws RemoteException;
}
