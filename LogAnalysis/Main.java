import java.io.*;
import java.util.*;


public class Main {
    public static void main(String[] args) {
        Set<Event> read = new HashSet<>();
        Set<Event> write = new HashSet<>();
        Set<Event> send_so = new HashSet<>();
        Set<Event> recv_so = new HashSet<>();
        Set<Event> rpc_send_pa = new HashSet<>();
        Set<Event> rpc_send_ch = new HashSet<>();
        Set<Event> rpc_recv_pa = new HashSet<>();
        Set<Event> rpc_recv_ch = new HashSet<>();


        File file = new File("output/log.csv");
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            for (String logFile : Config.INPUT_FILES) {
                Set<Event> fork_pa = new HashSet<>();
                Set<Event> fork_ch = new HashSet<>();
                Set<Event> join_pa = new HashSet<>();
                Set<Event> join_ch = new HashSet<>();
                Set<Event> lock = new HashSet<>();
                Set<Event> rel = new HashSet<>();

                HashMap<Integer, ArrayList<Event>> map = LogParser.convertLog(Config.INPUT_DIR, logFile);

                for (Map.Entry<Integer, ArrayList<Event>> entry : map.entrySet()) {
                    ArrayList<Event> eventList = entry.getValue();
                    for (int i = 0; i < eventList.size(); i++) {
                        String eventName = eventList.get(i).getType().toString();

                        switch (eventName) {
                            case "READ":
                                read.add(eventList.get(i));
                                break;
                            case "WRITE":
                                write.add(eventList.get(i));
                                break;
                            case "SEND_SO":
                                send_so.add(eventList.get(i));
                                break;
                            case "RECV_SO":
                                recv_so.add(eventList.get(i));
                                break;
                            case "RPC_SEND_PA":
                                rpc_send_pa.add(eventList.get(i));
                                break;
                            case "RPC_SEND_CH":
                                rpc_send_ch.add(eventList.get(i));
                                break;
                            case "RPC_RECV_PA":
                                rpc_recv_pa.add(eventList.get(i));
                                break;
                            case "RPC_RECV_CH":
                                rpc_recv_ch.add(eventList.get(i));
                                break;
                            case "FORK_PA":
                                fork_pa.add(eventList.get(i));
                                break;
                            case "FORK_CH":
                                fork_ch.add(eventList.get(i));
                                break;
                            case "JOIN_PA":
                                join_pa.add(eventList.get(i));
                                break;
                            case "JOIN_CH":
                                join_ch.add(eventList.get(i));
                                break;
                            case "LOCK":
                                lock.add(eventList.get(i));
                                break;
                            case "REL":
                                rel.add(eventList.get(i));
                                break;
                            default:
                                throw new IllegalArgumentException("Not defined EventType: " + eventName);
                        }

                        // ￿Last Event is unnecessary
                        if (i == eventList.size() - 1)
                            continue;

                        int eventHash = eventList.get(i).getHash();

                        pw.print(eventName + eventHash + "_" + eventList.get(i).getTraceId());
                        pw.print(",");
                        pw.println(eventList.get(i + 1).getType().toString() + eventList.get(i + 1).getHash() + "_" + eventList.get(i + 1).getTraceId());
                    }
                }
                checkEvent(fork_pa, fork_ch, pw);
                checkEvent(join_ch, join_pa, pw);
            }

            checkEvent(send_so, recv_so, pw);
            checkEvent(rpc_send_pa, rpc_recv_ch, pw);
            checkEvent(rpc_send_ch, rpc_recv_pa, pw);

            System.out.println("end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkEvent(Set<Event> startEvents, Set<Event> endEvents, PrintWriter pw) {
        for (Event e_start : startEvents) {
            boolean flag = true;
            for (Event e_end : endEvents) {
                if (e_start.getHash() == e_end.getHash()) {
                    pw.println(e_start.getType().toString() + e_start.getHash() + "_" + e_start.getTraceId() + "," + e_end.getType().toString() + e_end.getHash() + "_" + e_end.getTraceId());
                    flag = false;
                    break;
                }
            }
            if (flag) {
                System.err.println(e_start.getType().toString() + e_start.getHash());
//                System.exit(1);
            }

        }
    }
}