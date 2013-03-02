public class Client
    extends Endpoint
{
    public static void main(String[] args)
    {
        initializeDepartments("../departments.txt");
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

    @Command(description="sets the test phrase to be a given value", usage="<value>")
    public static void cmd_set_testphrase(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) master.setTestPhrase(joinStringArray(" ", args));
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="inserts the given student information into the database",
        usage="[id] <firstName> <lastName> <deptID> <year> <credits>")
    public static void cmd_insert(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) {
            String response = master.insertIntoDatabase("\"" + joinStringArray("\" \"", args) + "\"");
            log(response);
        }
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="inserts some test data into the database")
    public static void cmd_insert_testdata(Endpoint endpoint, String[] args)
    {
        final Student[] testData = new Student[] {
            new Student(1, "James", "King", getDepartment(150), 2, 200),
            new Student(3, "Max", "Whitby", getDepartment(150), 2, 199),
            new Student(4, "Jon", "Manson", getDepartment(150), 2, 201),
            new Student(7, "James", "Camden", getDepartment(150), 2, -80)
        };

        Host master = getMasterServer();
        if (master != null) {
            for (Student student : testData) {
                String response = master.insertIntoDatabase(student.serializeToString());
                log(response);
            }
        }
        else log(SERVERS_DOWN_MSG);
    }

    private static final String CONDITIONS_DESCRIPTION =
        "\n    <conditions> ::= <condition> | <condition> <connector> <conditions>\n" +
        "\n    <connector> ::= \"and\" | \"or\"\n" +
        "\n    <condition> ::= \"true\" | \"false\" | <fieldName> <operator> <value>\n" +
        "\n    <operator> ::= \"==\" | \"!=\" | \"<=\" | \">=\" | \"<\" | \">\"\n" +
        "\n    <fieldName> ::= \"identifier\" | \"firstName\" | \"lastName\"\n" +
          "                    | \"department\" | \"year\" | \"totalCredit\"\n" +
        "\nconditions are assumed to be in disjunctive normal form";

    private static final String ASSIGNMENTS_DESCRIPTION =
        "\n    <assignments> ::= <assignment> | <assignment> <assignments>\n" +
        "\n    <assignment> ::= <fieldName> \"=\" <value>\n";

    @Command(description="selects all students from the database that match a set of conditions\n"
        + CONDITIONS_DESCRIPTION,
        usage="<conditions>")
    public static void cmd_select(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) {
            String response = master.selectFromDatabase("\"" + joinStringArray("\" \"", args) + "\"");
            log(response);
        }
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="deletes all students from the database that match a set of conditions\n"
        + CONDITIONS_DESCRIPTION,
        usage="<conditions>")
    public static void cmd_delete(Endpoint endpoint, String[] args)
    {
        Host master = getMasterServer();
        if (master != null) {
            String response = master.deleteFromDatabase("\"" + joinStringArray("\" \"", args) + "\"");
            log(response);
        }
        else log(SERVERS_DOWN_MSG);
    }

    @Command(description="updates all students from the database that match a set of conditions\n"
        + ASSIGNMENTS_DESCRIPTION + CONDITIONS_DESCRIPTION,
        usage="<assignments> where <conditions>")
    public static void cmd_update(Endpoint endpoint, String[] args)
    {
        int whereIndex = 0;
        for (; whereIndex < args.length; ++ whereIndex) {
            if (args[whereIndex].equals("where")) break;
        }

        if (whereIndex == args.length) {
            log("Syntax error: Expected \"where\"");
            return;
        }

        Host master = getMasterServer();
        if (master != null) {
            String response = master.updateDatabase(
                "\"" + joinStringArray("\" \"", args, 0, whereIndex) + "\"",
                "\"" + joinStringArray("\" \"", args, whereIndex + 1, args.length - whereIndex - 1) + "\"");
            log(response);
        }
        else log(SERVERS_DOWN_MSG);
    }

    private Client() { }
}
