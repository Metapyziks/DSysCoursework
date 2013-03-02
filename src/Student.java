import java.text.MessageFormat;

public class Student
{
    private static String escape(String str)
    {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < str.length(); ++ i) {
            char c = str.charAt(i);
            switch(c) {
                case '"':
                case '\\':
                    buffer.append("\\");
                default:
                    buffer.append(c); break;
            }
        }
        return buffer.toString();
    }

    public static Student parse(Endpoint endpoint, String str)
    {
        String[] parsed = StructuredFileReader.parseLine(str, 6);

        if (parsed == null) {
            parsed = StructuredFileReader.parseLine("\"0\" " + str, 6);
        }

        if (parsed == null) return null;

        int deptID = Integer.parseInt(parsed[3]);
        Department department = endpoint.getDepartment(deptID);

        return new Student(
            Integer.parseInt(parsed[0]), parsed[1], parsed[2],
            department, Integer.parseInt(parsed[4]), Integer.parseInt(parsed[5]));
    }

    public int identifier;
    public String firstName;
    public String lastName;
    public Department department;
    public int year;
    public int totalCredit;

    public Student(int identifier, String firstName, String lastName,
        Department department, int year, int totalCredit)
    {
        this.identifier = identifier;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.year = year;
        this.totalCredit = totalCredit;
    }

    public String serializeToString()
    {
        return MessageFormat.format("\"{0}\" \"{1}\" \"{2}\" \"{3}\" \"{4}\" \"{5}\"",
            identifier, escape(firstName), escape(lastName),
            department.identifier, year, totalCredit);
    }

    @Override
    public String toString()
    {
        return MessageFormat.format("#{0} {1} {2}, {3}, year {4}, {5} credits",
            identifier, firstName, lastName, department.name, year, totalCredit);
    }
}
