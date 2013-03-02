public class QuerySyntaxException
    extends Exception
{
    public final int part;

    public QuerySyntaxException(String message, String[] split, int part)
    {
        super(message + " at argument #" + (part + 1) + " (" + split[part] + ")");

        this.part = part;
    }
}
