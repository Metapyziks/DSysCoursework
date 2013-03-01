import java.text.MessageFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

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

    private static BufferedReader _sReader;
    public static String readLine()
    {
        if (_sReader == null) {
            _sReader = new BufferedReader(new InputStreamReader(System.in));
        }

        try {
            return _sReader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
