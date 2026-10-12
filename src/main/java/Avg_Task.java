import java.util.ArrayList;
import java.util.List;

public class Avg_Task {
    private ArrayList<Integer> sir;
    public Avg_Task(ArrayList sir) {
        this.sir = sir;
    }
    public int avg(){
        int sum=0;
        for (Object s: sir){
            sum+=((Integer)s).intValue();
        }
        return sum/this.sir.size();
    }
}
