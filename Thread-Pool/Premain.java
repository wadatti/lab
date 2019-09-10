import java.lang.instrument.Instrumentation;

public class Premain {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        boolean flag = instrumentation.isNativeMethodPrefixSupported();



        System.out.println(flag);
        if (flag) {

        }
    }
}
