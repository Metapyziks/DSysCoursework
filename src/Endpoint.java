import java.text.MessageFormat;

public class Endpoint
{
    public static final String ADDRESS = "localhost";
    public static final int PORT = 3125;

    public static void log(String msg)
    {
        System.out.println(msg);
    }

    public static void log(String format, Object... arguments)
    {
        log(MessageFormat.format(format, arguments));
    }
}
