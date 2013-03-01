public class Client
    extends Endpoint
{
    public static void main(String[] args)
    {
        String hostsFilePath = args.length > 0 ? args[0] : "../hosts.txt";

        log("Reading hosts file \"{0}\"", hostsFilePath);
        Host[] hosts = Host.readFromFile(hostsFilePath);
        log("Found {0} host definitions", hosts.length);

        for (Host host : hosts) {
            try {
                host.connect();
                log("Connected to {0}", host);
            } catch (Exception e) {
                log("Failed to connect to {0}", host);
            }
        }
    }

    private Client() { }
}
