public class Client
    extends Endpoint
{
    public static void main(String[] args)
    {
        initializeHosts(args.length > 0 ? args[0] : "../hosts.txt");

        for (Host host : getHosts()) {
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
