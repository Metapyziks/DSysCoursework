import java.text.MessageFormat;

public class QueryResponse
{
    public final Student[] students;
    public final boolean success;

    public final String message;

    public QueryResponse(Student[] students)
    {
        this.students = students;

        success = true;
        message = null;
    }

    public QueryResponse(String message)
    {
        this.message = message;

        System.out.println(message);

        success = false;
        students = null;
    }

    public QueryResponse(String format, Object... args)
    {
        this(MessageFormat.format(format, args));
    }

    public String serializeToString()
    {
        StringBuffer buffer = new StringBuffer();
        if (success) {
            buffer.append("SUCCESS\n");
            for (Student student : students) {
                buffer.append(student.serializeToString());
                buffer.append("\n");
            }
        } else {
            buffer.append("FAILURE\n");
            buffer.append(message);
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
