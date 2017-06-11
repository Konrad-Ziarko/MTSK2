package shared;

import java.util.Random;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {
    public static double impatienceTimeMax = 1000;
    public static double impatienceTimeMin = 500;

    public int id;
    public int queueId;
    private Double oldFederateTime;
    private Double serviceTime;
    private boolean hasServiceFinished;
    private boolean privileged;
    public boolean wantsToChangeQueue;
    public boolean changedQueue;
    public Double impatienceTime;

    private void newImpatienceTime(){
        impatienceTime = new Random().nextDouble()*(impatienceTimeMax-impatienceTimeMin)+impatienceTimeMin;
    }

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = wantsToChangeQueue = changedQueue = false;
        newImpatienceTime();
    }


    public double getOldFederateTime() {
        return oldFederateTime;
    }


    public Double getServiceTime() {
        return serviceTime;
    }

    public void updateWithNewFederateTime(double newFederateTime) {
        if (serviceTime != null) {
            this.hasServiceFinished = newFederateTime - oldFederateTime >= serviceTime;
        }
    }
    public boolean checkImpatience(double newFederateTime){
        wantsToChangeQueue = newFederateTime - oldFederateTime >= impatienceTime && !changedQueue;
        return wantsToChangeQueue&& !changedQueue;
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
