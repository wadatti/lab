public class Event {
    private EventType type;
    private int tid;
    private int hash;
    private int pid;
    private String className;
    private String methodName;
    private int line;


    public Event(EventType type, int hash, int tid, int pid, String className, String methodName, int line) {
        this.type = type;
        this.hash = hash;
        this.tid = tid;
        this.pid = pid;
        this.className = className;
        this.methodName = methodName;
        this.line = line;
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

    public void setClassName(String className) {
        this.className = className;
    }

    public void setLine(int line) {
        this.line = line;
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

    public int getPid() {
        return pid;
    }

    public String getClassName() {
        return className;
    }

    public int getLine() {
        return line;
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
