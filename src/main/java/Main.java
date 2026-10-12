import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        ArrayList<Integer> sir=new ArrayList<>();
        sir.add(1);
        sir.add(2);
        sir.add(3);
        sir.add(4);
        sir.add(5);
        Avg_Task avg= new Avg_Task(sir);
        System.out.printf("%d",avg.avg());
    }
}
