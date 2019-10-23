import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class LogParser {
    public static HashMap<Integer, LinkedList<Event>> convertLog(String dir, String file) throws IOException {
        HashMap<Integer, LinkedList<Event>> map = new HashMap<>();
        int nodeID = 0;
        nodeID++;
        System.out.println("parse: " + file);
        BufferedReader br = new BufferedReader(new FileReader(dir + file));
        String line;

        int num = 0;
        while ((line = br.readLine()) != null) {
            num++;
            // event, hash, tid, pid, className, line
            StringTokenizer st = new StringTokenizer(line, ",");
            Event.EventType type = Event.EventType.getEnum(st.nextToken());
            int hash = Integer.parseInt(st.nextToken());
            int tid = Integer.parseInt(st.nextToken());
            int pid = Integer.parseInt(st.nextToken());


            if (hash == 0)
                continue;

            if (type == Event.EventType.FORK_PA || type == Event.EventType.FORK_CH || type == Event.EventType.JOIN_CH || type == Event.EventType.JOIN_PA) {
                hash += nodeID;
            }
            Event event = new Event(type, hash, tid);

            if (st.hasMoreTokens()) {
                event.setClassName(st.nextToken());
                event.setLine(Integer.parseInt(st.nextToken()));
            }

            LinkedList<Event> list;
            if (map.containsKey(tid)) {
                list = map.get(tid);
            } else {
                list = new LinkedList<>();
                map.put(tid, list);
            }
            list.add(event);
        }


        return map;
    }
}
