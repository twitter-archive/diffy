public class DockerServerCandidate {

    public static void main(String[] args) throws Exception {
        int primary = Integer.parseInt(args[0]);
        Thread p = new Thread(() -> ExampleUtils.bind(primary, x -> x.toUpperCase()));
        p.start();
        while(true){
            Thread.sleep(10);
        }
    }
}