public class Event {
    private EventType type;
    private int tid;
    private int traceId;
    private int hash;
    private int pid;


    public Event(EventType type, int hash, int traceId, int tid, int pid) {
        this.type = type;
        this.hash = hash;
        this.traceId = traceId;
        this.tid = tid;
        this.pid = pid;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }
    
    public EventType getType() {
        return type;
    }

    public int getTid() {
        return tid;
    }

    public int getHash() {
        return hash;
    }

    public int getTraceId() {
        return traceId;
    }

    public int getPid() {
        return pid;
    }

    public enum EventType {
        READ("READ"),
        WRITE("WRITE"),
        SEND_SO("SEND_SO"),
        RECV_SO("RECV_SO"),
        RPC_SEND_PA("RPC_SEND_PA"),
        RPC_SEND_CH("RPC_SEND_CH"),
        RPC_RECV_PA("RPC_RECV_PA"),
        RPC_RECV_CH("RPC_RECV_CH"),
        FORK_PA("FORK_PA"),
        FORK_CH("FORK_CH"),
        JOIN_PA("JOIN_PA"),
        JOIN_CH("JOIN_CH"),
        LOCK("LOCK"),
        REL("REL"),
        Udef("Undefined"),
        ;

        private final String text;

        EventType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static EventType getEnum(String str) {
            for (EventType type : EventType.values()) {
                if (str.equals(type.toString())) {
                    return type;
                }
            }

            System.err.println("Not Found EventType...:" + str);
            System.exit(1);
            throw new IllegalArgumentException();
        }
    }
}
