import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Created by rg on 06-Feb-17.
 */
public class ShiftTest {
    public static void main(String[] args) throws Exception {
        String outputString = com.google.common.io.Files.toString(new File("C:\\test.txt"), StandardCharsets.UTF_8);
        System.out.println(outputString);

    }
}
