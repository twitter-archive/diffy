public class DockerServerProduction{

    public static void main(String[] args) throws Exception {
        int primary = Integer.parseInt(args[0]);
        Thread p = new Thread(() -> ExampleUtils.bind(primary, x -> x.toLowerCase()));
        p.start();
        while(true){
            Thread.sleep(10);
        }
    }
}