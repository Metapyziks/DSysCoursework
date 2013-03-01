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

    public static Student parseString(Endpoint endpoint, String str)
    {
        String[] parsed = StructuredFileReader.parseLine(str, 6);

        int deptID = Integer.parseInt(parsed[3]);
        Department department = endpoint.getDepartment(deptID);

        return new Student(
            Integer.parseInt(parsed[0]), parsed[1], parsed[2],
            department, Integer.parseInt(parsed[4]), Integer.parseInt(parsed[5]));
    }

    private int _identifier;
    private String _firstName;
    private String _lastName;
    private Department _department;
    private int _year;
    private int _totalCredit;

    public Student(int identifier, String firstName, String lastName,
        Department department, int year, int totalCredit)
    {
        _identifier = identifier;
        _firstName = firstName;
        _lastName = lastName;
        _department = department;
        _year = year;
        _totalCredit = totalCredit;
    }

    public int getIdentifier()
    {
        return _identifier;
    }
    
    public void setIdentifier(int value)
    {
        _identifier = value;
    }   
     
    public String getFirstName()
    {
        return _firstName;
    }
    
    public void setFirstName(String value)
    {
        _firstName = value;
    }   
     
    public String getLastName()
    {
        return _lastName;
    }
    
    public void setLastName(String value)
    {
        _lastName = value;
    }   
     
    public Department getDepartment()
    {
        return _department;
    }
    
    public void setDepartment(Department value)
    {
        _department = value;
    }   
     
    public int getYear()
    {
        return _year;
    }
    
    public void setYear(int value)
    {
        _year = value;
    }   
     
    public int getTotalCredits()
    {
        return _totalCredit;
    }
    
    public void setTotalCredits(int value)
    {
        _totalCredit = value;
    }

    public String serializeToString()
    {
        return MessageFormat.format("\"{0}\" \"{1}\" \"{2}\" \"{3}\" \"{4}\" \"{5}\"",
            _identifier, escape(_firstName), escape(_lastName),
            _department.identifier, _year, _totalCredit);
    }
}
