import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListGet {
    private static List<File> files=new ArrayList<>();

    public static List<File> getFiles(File rootFile) {
        for (File file : rootFile.listFiles()) {
            if (file.isDirectory()) {
                getFiles(file);
            } else {
                if (file.getName().endsWith(".class") || file.getName().endsWith(".jar"))
                    files.add(file);
            }
        }
        return files;
    }
}
