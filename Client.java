public class Client implements Cloneable {
    private final int id;
    private final String name;
    private int rank;

    public Client(String name, int rank) {
        this.id = -1;
        this.name = name;
        this.rank = rank;
    }

    public Client(int id, String name, int rank) {
        this.id = id;
        this.name = name;
        this.rank = rank;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    @Override
    public String toString() {
        return String.format("Client{id=%d, name='%s', rank=%d}", id, name, rank);
    }

    @Override
    protected Client clone() throws CloneNotSupportedException {
        return (Client) super.clone();
    }
}