package shared;

import java.util.Random;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {

    public Integer id;
    public void setId (Integer id){
        this.id = id;
    }
    public Integer queueId;
    private Double oldFederateTime;
    private Double serviceTime;
    private Boolean hasServiceFinished;
    private Boolean privileged;
    public Integer nrSprawy;

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = false;
        this.nrSprawy = new Random().nextInt(10)+1;
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

    public Integer getId() {
        return id;
    }

    public void setServiceTime(double serviceTime) {
        this.serviceTime = serviceTime;
    }
}
