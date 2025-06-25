package philosopher;

public class Table {
    private final int numberOfPhilosophers;
    private final Fork[] forks;

    public Table(int numberOfPhilosophers) {
        this.numberOfPhilosophers = numberOfPhilosophers;
        this.forks = new Fork[numberOfPhilosophers];
        for (int i = 0; i < numberOfPhilosophers; i++) {
            forks[i] = new Fork(i);
        }
    }

    public Fork getFork(int index) {
        return forks[index];
    }

    public int getNumberOfPhilosophers() {
        return numberOfPhilosophers;
    }
}
