package philosopher;

public class Fork {
    private final int id;
    private boolean isAvailable;
    
    public Fork(int id) {
        this.id = id;
        this.isAvailable = true;
    }
    
    public synchronized boolean pickUp() {
        if (isAvailable) {
            isAvailable = false;
            return true;
        }
        return false;
    }
    
    public synchronized void putDown() {
        isAvailable = true;
        notifyAll(); 
    }
    
    public synchronized void waitForFork() throws InterruptedException {
        while (!isAvailable) {
            wait();
        }
        isAvailable = false;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
}
