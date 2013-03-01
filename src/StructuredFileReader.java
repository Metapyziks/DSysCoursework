import java.io.*;
import java.util.*;

public class StructuredFileReader
{
    public static String[] parseLine(String line, int partCount)
    {
        String[] parts = new String[partCount];
        for (int p = 0; p < partCount; ++ p) parts[p] = "";

        int part = 0, i = 0;
        boolean inPart = false, escaped = false;

        while (part < partCount && i < line.length()) {
            char c = line.charAt(i++);
            if (escaped) {
                if (inPart) parts[part] += c;
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                if (inPart) ++part;
                inPart = !inPart;
            } else if (inPart) {
                parts[part] += c;
            } else if (c == '#') {
                break;
            }
        }

        if (part < partCount) return null;

        return parts;
    }

    public static String[][] readFromFile(String filePath, int partCount)
    {
        ArrayList<String[]> items = new ArrayList<String[]>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] item = parseLine(line, partCount);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[][] arr = new String[items.size()][];
        items.toArray(arr);

        return arr;
    }
}
