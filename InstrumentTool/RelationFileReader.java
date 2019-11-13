import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * ターゲットJarファイルの関連jarファイル or ディレクトリを集めるクラス
 */
public class RelationFileReader {
    private File[] files;
    private List<String> paths;

    public RelationFileReader(File inputDir) {
        files = inputDir.listFiles();
        paths = new LinkedList<>();
    }

    public List<String> getPaths() {
        collect();
        return paths;
    }

    private void collect() {
        for (File file : files) {
            collectPath(file);
        }
    }

    private void collectPath(File file) {
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            for (File subfile : subfiles) {
                collectPath(subfile);
            }
        } else if (file.isFile() && file.getPath().endsWith(".jar")) {
            paths.add(file.getPath());
        } else if (file.isFile() && file.getPath().endsWith(".class")) {
            paths.add(file.getPath().replaceFirst("/[0-9a-zA-Z]*.class$", ""));
        }
    }
}
