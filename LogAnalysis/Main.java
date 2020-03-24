import java.io.*;
import java.util.*;


public class Main {
    public static void main(String[] args) {
        Map<Integer, HashMap<Integer, ArrayList<Event>>> eventMap = new HashMap<>();
        Set<Integer> read = new HashSet<>();
        Set<Integer> write = new HashSet<>();
        Set<Integer> send_so = new HashSet<>();
        Set<Integer> recv_so = new HashSet<>();
        Set<Integer> rpc_send_pa = new HashSet<>();
        Set<Integer> rpc_send_ch = new HashSet<>();
        Set<Integer> rpc_recv_pa = new HashSet<>();
        Set<Integer> rpc_recv_ch = new HashSet<>();
        Set<Integer> fork_pa = new HashSet<>();
        Set<Integer> fork_ch = new HashSet<>();
        Set<Integer> join_pa = new HashSet<>();
        Set<Integer> join_ch = new HashSet<>();
        Set<Integer> lock = new HashSet<>();
        Set<Integer> rel = new HashSet<>();

        int file_num = 0;
        File file = new File("output/log.csv");
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            for (String logFile : Config.INPUT_FILES) {
                file_num++;

                HashMap<Integer, ArrayList<Event>> map = LogParser.convertLog(Config.INPUT_DIR, logFile);
                eventMap.put(file_num, map);

                for (Map.Entry<Integer, ArrayList<Event>> entry : map.entrySet()) {
                    ArrayList<Event> eventList = entry.getValue();
                    for (int i = 0; i < eventList.size(); i++) {
                        String eventName = eventList.get(i).getType().toString();
                        int eventHash = eventList.get(i).getHash();

                        switch (eventName) {
                            case "READ":
                                read.add(eventHash);
                                break;
                            case "WRITE":
                                write.add(eventHash);
                                break;
                            case "SEND_SO":
                                send_so.add(eventHash);
                                break;
                            case "RECV_SO":
                                recv_so.add(eventHash);
                                break;
                            case "RPC_SEND_PA":
                                rpc_send_pa.add(eventHash);
                                break;
                            case "RPC_SEND_CH":
                                rpc_send_ch.add(eventHash);
                                break;
                            case "RPC_RECV_PA":
                                rpc_recv_pa.add(eventHash);
                                break;
                            case "RPC_RECV_CH":
                                rpc_recv_ch.add(eventHash);
                                break;
                            case "FORK_PA":
                                fork_pa.add(eventHash);
                                break;
                            case "FORK_CH":
                                fork_ch.add(eventHash);
                                break;
                            case "JOIN_PA":
                                join_pa.add(eventHash);
                                break;
                            case "JOIN_CH":
                                join_ch.add(eventHash);
                                break;
                            case "LOCK":
                                lock.add(eventHash);
                                break;
                            case "REL":
                                rel.add(eventHash);
                                break;
                            default:
                                throw new IllegalArgumentException("Not defined EventType: " + eventName);
                        }

                        // ￿Last Event is unnecessary
                        if (i == eventList.size() - 1)
                            continue;
                        // Omit redundant Event
                        if (eventName.equals(eventList.get(i + 1).getType().toString()) && eventHash == eventList.get(i + 1).getHash())
                            continue;

                        pw.print(eventName + eventHash);
                        pw.print(",");
                        pw.println(eventList.get(i + 1).getType().toString() + eventList.get(i + 1).getHash());
                    }
                }

            }

            checkEvent(send_so, recv_so, "SEND_SO", "RECV_SO", pw);
            checkEvent(rpc_send_pa, rpc_recv_ch, "RPC_SEND_PA", "RPC_RECV_CH", pw);
            checkEvent(rpc_send_ch, rpc_recv_pa, "RPC_SEND_CH", "RPC_RECV_PA", pw);
            checkEvent(fork_pa, fork_ch, "FORK_PA", "FORK_CH", pw);
            checkEvent(join_ch, join_pa, "JOIN_CH", "JOIN_PA", pw);
            checkEvent(lock, rel, "LOCK", "REL", pw);

            System.out.println("end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkEvent(Set<Integer> startEvents, Set<Integer> endEvents, String startEventName, String endEventName, PrintWriter pw) {
        for (Integer hash : startEvents) {
            if (endEvents.contains(hash)) {
                pw.println(startEventName + hash + "," + endEventName + hash);
            } else {
                System.err.println(startEventName + hash);
//                System.exit(1);
            }
        }
    }
}