

public class ExampleServers {
    public static void main(String[] args) throws Exception {
        int primary = Integer.parseInt(args[0]);
        int secondary = Integer.parseInt(args[1]);
        int candidate = Integer.parseInt(args[2]);
        Thread p = new Thread(() -> ExampleUtils.bind(primary, x -> x.toLowerCase()));
        Thread s = new Thread(() -> ExampleUtils.bind(secondary, x -> x.toLowerCase()));
        Thread c = new Thread(() -> ExampleUtils.bind(candidate, x -> x.toUpperCase()));
        p.start();
        s.start();
        c.start();
        while(true){
            Thread.sleep(10);
        }
    }
}

