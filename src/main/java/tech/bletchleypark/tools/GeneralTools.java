package tech.bletchleypark.tools;


public class GeneralTools {
    public static void pause(double durationSeconds){
        try {
            Thread.sleep((long) (durationSeconds*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
