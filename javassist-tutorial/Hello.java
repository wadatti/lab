public class Hello {
    public void say() {
        System.out.println("Hello");
    }

    public void hi() {
        System.out.println("Hi");
    }

    public static void main(String[] args) {
        System.out.println("start...");
        new Hello().say();
    }
}