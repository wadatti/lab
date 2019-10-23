public class Event {
    private EventType type;
    private int tid;
    private int hash;
    private int pid;
    private String className;
    private int line;


    public Event(EventType type, int hash, int tid) {
        this.type = type;
        this.hash = hash;
        this.tid = tid;
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
        SEND_RPC("SEND_RPC"),
        RECV_RPC("RECV_RPC"),
        BEGIN_RPC("BEGIN_RPC"),
        END_RPC("END_RPC"),
        FORK_PA("FORK_PA"),
        FORK_CH("FORK_CH"),
        JOIN_PA("JOIN_PA"),
        JOIN_CH("JOIN_CH"),
        LOCK("LOCK"),
        REL("REL"),
        CREATE("CREATE_EV"),
        BEGIN("BEGIN_EV"),
        END("END_EV"),
        INIT("Initial"),
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

            // 一致する名前のenumが見当たらなかった場合
            return EventType.Udef;
        }

    }

}
