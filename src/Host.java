import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;

public class Host
{
    private static Host parseLine(String line)
    {
        String[] parts = new String[] { "", "", "" };
        int part = 0, i = 0;
        boolean inPart = false, escaped = false;

        while (part < 3 && i < line.length()) {
            char c = line.charAt(i++);
            if (escaped) {
                if (inPart) parts[part] += c;
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                if (inPart) ++part;
                inPart = !inPart;
            } else if (inPart) {
                parts[part] += c;
            }
        }

        if (part < 3) return null;

        return new Host(parts[0], parts[1], Integer.parseInt(parts[2]));
    }

    public static Host[] readFromFile(String filePath)
    {
        ArrayList<Host> hosts = new ArrayList<Host>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                Host host = Host.parseLine(line);
                if (host != null) {
                    hosts.add(host);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Host[] arr = new Host[hosts.size()];
        hosts.toArray(arr);

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

    public IDatabaseConnection getDatabaseConnection()
    {
        return _dbcon;
    }

    @Override
    public String toString()
    {
        return name + "(" + address + ":" + Integer.toString(port) + ")";
    }
}
