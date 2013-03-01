public class Department
{
    public static Department[] readFromFile(String filePath)
    {
        String[][] parsed = StructuredFileReader.readFromFile(filePath, 2);
        Department[] arr = new Department[parsed.length];

        for (int i = 0; i < parsed.length; ++ i) {
            arr[i] = new Department(Integer.parseInt(parsed[i][0]), parsed[i][1]);
        }

        return arr;
    }

    public final int identifier;
    public final String name;

    private Department(int identifier, String name)
    {
        this.identifier = identifier;
        this.name = name;
    }
}
