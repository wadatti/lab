public class Operation implements Runnable {
    Account account;

    public Operation(Account account) {
        this.account = account;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            if (i % 2 == 0) account.deposit(200);
            else account.withdraw(200);
        }
    }
}
