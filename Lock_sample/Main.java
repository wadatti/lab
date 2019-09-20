public class Main {

    public static void main(String[] args) {
        Account account = new Account();
        Operation ope = new Operation(account);
        Thread t1 = new Thread(ope);
        Thread t2 = new Thread(ope);
        Thread t3 = new Thread(ope);
        t1.start();
        t2.start();
        t3.start();
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("balance = " + account.balance);
    }
}
