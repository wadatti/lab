import java.io.*;
import java.util.*;


public class Main {
    public static Map<Integer, HashMap<Integer, LinkedList<Event>>> eventMap = new HashMap<>();
    public static Set<Integer> sendRPCs = new HashSet<>();
    public static Set<Integer> beginRPCs = new HashSet<>();
    public static Set<Integer> endRPCs = new HashSet<>();
    public static Set<Integer> recvRPCs = new HashSet<>();
    public static Set<Integer> forkPA = new HashSet<>();
    public static Set<Integer> forkCH = new HashSet<>();
    public static Set<Integer> joinPA = new HashSet<>();
    public static Set<Integer> joinCH = new HashSet<>();

    public static void main(String[] args) {
        int file_num = 0;
        File file = new File("output/log.csv");
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            for (String logFile : Config.INPUT_FILES) {
                file_num++;

                HashMap<Integer, LinkedList<Event>> map = LogParser.convertLog(Config.INPUT_DIR, logFile);
                eventMap.put(file_num, map);

                for (Map.Entry<Integer, LinkedList<Event>> entry : map.entrySet()) {
                    LinkedList<Event> eventList = entry.getValue();
                    for (int i = 0; i < eventList.size(); i++) {
                        String eventName = eventList.get(i).getType().toString();
                        int eventHash = eventList.get(i).getHash();

                        if (eventName.equals("SEND_RPC")) {
                            sendRPCs.add(eventHash);
                        } else if (eventName.equals("BEGIN_RPC")) {
                            beginRPCs.add(eventHash);
                        } else if (eventName.equals("END_RPC")) {
                            endRPCs.add(eventHash);
                        } else if (eventName.equals("RECV_RPC")) {
                            recvRPCs.add(eventHash);
                        } else if (eventName.equals("FORK_PA")) {
                            forkPA.add(eventHash);
                        } else if (eventName.equals("FORK_CH")) {
                            forkCH.add(eventHash);
                        } else if (eventName.equals("JOIN_PA")) {
                            joinPA.add(eventHash);
                        } else if (eventName.equals("JOIN_CH")) {
                            joinCH.add(eventHash);
                        }

                        if (i == eventList.size() - 1)
                            continue;
                        if (eventName.equals(eventList.get(i + 1).getType().toString()) && eventHash == eventList.get(i + 1).getHash())
                            continue;

                        pw.print(eventName + eventHash);
                        pw.print(",");
                        pw.println(eventList.get(i + 1).getType().toString() + eventList.get(i + 1).getHash());
                    }
                }

            }


            for (Integer hash : sendRPCs) {
                if (beginRPCs.contains(hash)) {
                    pw.println("SEND_RPC" + hash + "," + "BEGIN_RPC" + hash);
                } else {
                    System.err.println("SEND_RPC" + hash);
//                    System.exit(1);
                }
            }

            for (Integer hash : endRPCs) {
                if (recvRPCs.contains(hash)) {
                    pw.println("END_RPC" + hash + "," + "RECV_RPC" + hash);
                } else {
                    System.err.println("END_RPC" + hash);
//                    System.exit(1);
                }
            }

            for (Integer hash : forkPA) {
                if (forkCH.contains(hash)) {
                    pw.println("FORK_PA" + hash + "," + "FORK_CH" + hash);
                } else {
                    System.err.println("FORK_PA" + hash);
//                    System.exit(1);
                }
            }

            //必ずしもJOIN＿CHに対してFORK＿PAがあるとは限らない
            for (Integer hash : joinCH) {
                if (joinPA.contains(hash)) {
                    pw.println("JOIN_CH" + hash + "," + "JOIN_PA" + hash);
                } else {
                    System.err.println("JOIN_CH" + hash);
//                    System.exit(1);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
