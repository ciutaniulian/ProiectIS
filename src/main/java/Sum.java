public class Sum {
    public int calcSum(int[] elem){
        int suma=0;
        for(int e: elem)
        {
            suma += e;
        }
        return suma;
    }

    public static void main(String[] args) {
        Sum suma=new Sum();
        int[] sir={1,2,3,4,5,6,7};
        int rez=suma.calcSum(sir);
        System.out.println("suma este "+rez);
    }
}
