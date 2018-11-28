package il.ac.colman.cs;

import java.io.File;
import java.util.UUID;

public class Screenshot {
    static String take(String url) {
        UUID uuid = UUID.randomUUID();
        String screenshotJs = new File("screenshot", "screenshot.js").toString();
        String filename = new File("screenshots", uuid.toString() + ".png").toString();

        String[] cmd = {"node", screenshotJs, url, filename};

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();

            return filename;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
