package shared;

import java.util.Random;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {
    private Double oldFederateTime;
    private Double serviceTime;
    private boolean hasServiceFinished;
    private boolean privileged;
    private Double impatienceTime;

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = false;
        impatienceTime = new Random().nextDouble()*400+50;
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
        //jesli czas zniecierpliwienia minal to zmien kolejke i przedluz czas zniecierpliwienia
        //else if ()

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
