public class Client
    extends Endpoint
{
    private static final String SERVERS_DOWN_MSG = "All servers are down";

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

    @Command(description="sends a handshake message to the server and waits for a reply")
    public static void cmd_ping(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) log(master.ping());
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="gets the current value of the test phrase")
    public static void cmd_get_testphrase(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) log(master.getTestPhrase());
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="sets the test phrase to be a given value")
    public static void cmd_set_testphrase(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) master.setTestPhrase(joinStringArray(" ", args));
        else log(SERVERS_DOWN_MSG);
    }

    private Client() { }
}
