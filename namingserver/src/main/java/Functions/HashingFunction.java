package Functions;

public class HashingFunction {

    public static int hashNodeName(String nodeName) {
        int hash = 0;
        for (int i = 0; i < nodeName.length(); i++) {
            hash = 31 * hash + nodeName.charAt(i); // A better approach for string hashing
        }
        return (Math.abs(hash) % 32768); // Ensure the result is within the 0-32768 range
    }
}
