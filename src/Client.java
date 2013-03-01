public class Client
    extends Endpoint
{
    public static void main(String[] args)
    {
        initializeHosts(args.length > 0 ? args[0] : "../hosts.txt");

        Client client = new Client();

        try {
            readConsoleInput(client);
        } catch (Exception e) {
            log("An {0} has occurred:", e.toString());
            e.printStackTrace();
        }
    }

    private Client() { }
}
