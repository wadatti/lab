import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
    public static HashMap<Integer, ArrayList<Event>> convertLog(String dir, String file) throws IOException {
        HashMap<Integer, ArrayList<Event>> map = new HashMap<>();
        String regex = "([a-zA-Z]+)([0-9]+).csv";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(file);
        if (!m.matches()) {
            System.out.println("cannot match!: " + file);
            System.exit(1);
        }

        int nodeID = Integer.parseInt(m.group(2));

        System.out.println("parse: " + file);
        BufferedReader br = new BufferedReader(new FileReader(dir + file));
        String line;

        while ((line = br.readLine()) != null) {
            // event, hash, tid, pid, className, line
            StringTokenizer st = new StringTokenizer(line, ",");
            Event.EventType type = Event.EventType.getEnum(st.nextToken());
            int hash = Integer.parseInt(st.nextToken());
            int tid = Integer.parseInt(st.nextToken());
            int pid = Integer.parseInt(st.nextToken());
            String className = st.nextToken();
            String methodName = st.nextToken();
            int lineNum = Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) {
                System.out.println(st.nextToken());
                throw new IllegalArgumentException();
            }


            if (hash == 0) {
//                System.err.println("hash code is 0!");
//                throw new IllegalArgumentException();
                continue;
            }


            if (type == Event.EventType.FORK_PA || type == Event.EventType.FORK_CH || type == Event.EventType.JOIN_CH || type == Event.EventType.JOIN_PA) {
                hash += nodeID;
            }
            Event event = new Event(type, hash, tid, pid, className, methodName, lineNum);

            ArrayList<Event> list;
            if (map.containsKey(tid)) {
                list = map.get(tid);
            } else {
                list = new ArrayList<>();
                map.put(tid, list);
            }
            list.add(event);
        }
        return map;
    }
}
