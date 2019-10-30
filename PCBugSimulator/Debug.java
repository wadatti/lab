
public class Debug {
    public static synchronized void print(String str) {
        System.out.println("\t" + str);
    }

    public static synchronized void print(String str, long tid) {
        System.out.println("@" + tid + "\t" + str);
    }


    public static synchronized void printErr(String str) {
        System.err.println("\t" + str);
    }

    public static synchronized void printClass(String className) {
        System.out.println(className + ".class");
    }
}
