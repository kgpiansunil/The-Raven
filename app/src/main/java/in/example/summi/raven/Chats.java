package in.example.summi.raven;

public class Chats {


    private String seen;

    public Chats(){

    }

    public Chats(String seen, long timestamp) {
        this.seen = seen;
        this.timestamp = timestamp;
    }

    private long timestamp;

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
