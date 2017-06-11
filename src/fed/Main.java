package fed;

/**
 * Created by Konrad on 11.06.2017.
 */
public class Main {

    public static void main(String[] args){
        new Thread(()->{
            new FederatKasa().runFederate();
        }).start();
        new Thread(()->{
            new FederatKlient().runFederate();
        }).start();
        new Thread(()->{
            new FederatGUI().runFederate();
        }).start();
    }
}
