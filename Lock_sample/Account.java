import java.util.concurrent.locks.ReentrantLock;

public class Account {
    int balance = 10000;
    ReentrantLock lock = new ReentrantLock();

    void deposit(int amount) {
        try {
            lock.lock();
            balance += amount;
        } finally {
            lock.unlock();
        }
    }

    void withdraw(int amount) {
        try {
            lock.lock();
            balance -= amount;
        } finally {
            lock.unlock();
        }
    }
}
