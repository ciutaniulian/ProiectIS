package ro.expensesplitter;

import java.util.ArrayList;

public class MeanByIndex {
    public int doMeanOp(ArrayList<Integer> myList)
    {
        int num = 0;

        int sumPondere = 0;
        for(int i = 0; i < myList.toArray().length; i++) {
            num += i*myList.get(i);
            sumPondere += i;
        }

        return num/sumPondere;

    }
}
