package shared;

import java.util.Random;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {
    public static double impatienceTimeMax = 3000;
    public static double impatienceTimeMin = 2000;

    public int id;
    private Double oldFederateTime;
    private Double serviceTime;
    private boolean hasServiceFinished;
    private boolean privileged;
    public boolean wantsToChangeQueue;
    private Double impatienceTime;

    private void newImpatienceTime(){
        impatienceTime = new Random().nextDouble()*(impatienceTimeMax-impatienceTimeMin)+impatienceTimeMin;
    }

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = wantsToChangeQueue = false;
        newImpatienceTime();
    }


    public double getOldFederateTime() {
        return oldFederateTime;
    }


    public Double getServiceTime() {
        return serviceTime;
    }

    public String updateWithNewFederateTime(double newFederateTime) {
        if (serviceTime != null) {
            this.hasServiceFinished = newFederateTime - oldFederateTime >= serviceTime;
            return "";
        }
        if (impatienceTime != null) {
            this.wantsToChangeQueue = newFederateTime - oldFederateTime >= impatienceTime;
            newImpatienceTime();
            return "";
        }
        //jesli czas zniecierpliwienia minal to zmien kolejke i przedluz czas zniecierpliwienia
        //else if ()
        return "";
    }

    public boolean hasServiceFinished() {
        return hasServiceFinished;
    }

    public void setOldFederateTime(double oldFederateTime) {
        this.oldFederateTime = oldFederateTime;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean priviledged) {
        this.privileged = priviledged;
    }
}
