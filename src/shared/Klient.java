package shared;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {
    private Double oldFederateTime;
    private Double serviceTime;
    private boolean hasServiceFinished;
    private boolean privileged;

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = false;
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
