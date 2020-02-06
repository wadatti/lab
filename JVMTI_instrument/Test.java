class Foo {
    synchronized void do_wait() {
        System.out.println("I am do_wait: 1");
        try {
            wait();
        } catch (Exception e) {
        }
        System.out.println("I am do_wait: 2");
    }

    synchronized void do_notify() {
        System.out.println("I am do_notify: 1");
        notify();
        System.out.println("I am do_notify: 2");
    }
}

public class Test implements Runnable {
    int num;
    static Foo foo = new Foo();

    public void run() {
        if (num == 1) {
            foo.do_wait();
        } else {
            foo.do_notify();
        }
    }

    Test(int num) {
        this.num = num;
    }

    public static void main(String[] args) {
        Thread th1 = new Thread(new Test(1));
        Thread th2 = new Thread(new Test(2));
        th1.start();
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        th2.start();
        try {
            th1.join();
            th2.join();
        } catch (Exception e) {
        }
    }
}
