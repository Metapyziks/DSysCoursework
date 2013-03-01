import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;

public class Host
    implements IDatabaseConnection
{
    public static Host[] readFromFile(String filePath)
    {
        String[][] parsed = StructuredFileReader.readFromFile(filePath, 3);
        Host[] arr = new Host[parsed.length];
        
        for (int i = 0; i < parsed.length; ++ i) {
            arr[i] = new Host(parsed[i][0], parsed[i][1], Integer.parseInt(parsed[i][2]));
        }

        return arr;
    }

    private Registry _registry;
    private IDatabaseConnection _dbcon;

    public final String name;
    public final String address;
    public final int port;

    private Host(String name, String address, int port)
    {
        _registry = null;
        _dbcon = null;

        this.name = name;
        this.address = address;
        this.port = port;
    }

    public void connect()
        throws RemoteException, NotBoundException
    {
        _registry = LocateRegistry.getRegistry(address, port);
        _dbcon = (IDatabaseConnection) _registry.lookup("DatabaseConnection");
    }

    public void disconnect()
    {
        _registry = null;
        _dbcon = null;
    }

    public boolean isConnected()
    {
        try {
            return _dbcon != null && _dbcon.getIdentifier() > -1;
        } catch(RemoteException e) {
            return false;
        }
    }

    public <T> T attemptInvokeRemote(int history, Object... args)
        throws RemoteException, IllegalAccessException, InvocationTargetException
    {
        try {
            if (!isConnected()) connect();
        } catch (NotBoundException e) {
            throw new RemoteException();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callerName = stackTrace[history + 2].getMethodName();
        Method caller = null;

        Method[] methods = _dbcon.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(callerName)) {
                caller = method;
                break;
            }
        }

        try {
            return (T) caller.invoke(_dbcon, args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RemoteException) {
                disconnect();
                throw (RemoteException) targetException;
            }
            throw e;
        }
    }

    @Override
    public int getIdentifier()
    {
        try { return this.<Integer>attemptInvokeRemote(0); }
        catch (Exception e) { return -1; }
    }

    @Override
    public String ping()
    {
        try { return this.<String>attemptInvokeRemote(0); }
        catch (Exception e) { return null; }
    }

    @Override
    public void requestSync(int identifier)
    {
        try { this.<Object>attemptInvokeRemote(0, identifier); }
        catch (Exception e) { }
    }

    @Override
    public String getTestPhrase()
    {
        try { return this.<String>attemptInvokeRemote(0); }
        catch (Exception e) { return null; }
    }

    @Override
    public void setTestPhrase(String phrase)
    {
        try { this.<Object>attemptInvokeRemote(0, phrase); }
        catch (Exception e) { }
    }

    @Override
    public String insertIntoDatabase(String student)
    {
        try { return this.<String>attemptInvokeRemote(0, student); }
        catch (Exception e) { return null; }
    }

    @Override
    public String queryDatabase(String query)
    {
        try { return this.<String>attemptInvokeRemote(0, query); }
        catch (Exception e) { return null; }
    }

    @Override
    public String toString()
    {
        return name + "@" + address + ":" + Integer.toString(port);
    }
}
