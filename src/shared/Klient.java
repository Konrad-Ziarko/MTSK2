package shared;

import java.util.Random;

/**
 * Created by konrad on 5/29/17.
 */
public class Klient {

    private Integer id;
    public void setId (Integer id){
        this.id = id;
    }

    private Double patienceTime;
    private Integer queueId;
    private Integer queuePosition;
    private Double oldFederateTime;
    private Double serviceTime;
    private Boolean hasServiceFinished;
    private Boolean privileged;
    private Integer nrSprawy;

    public Klient(double oldFederateTime, int serviceTime, double patienceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = false;
        queuePosition = -1;
        this.patienceTime = patienceTime;
        this.nrSprawy = new Random().nextInt(10)+1;
    }

    public boolean wantsToLeave(double newFederateTime){
        return newFederateTime - oldFederateTime >= patienceTime;
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

    public Integer getQueueId() {
        return queueId;
    }

    public void setQueueId(Integer queueId) {
        this.queueId = queueId;
    }

    public Integer getNrSprawy() {
        return nrSprawy;
    }

    public void setNrSprawy(int nrSprawy) {
        this.nrSprawy = nrSprawy;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void addToPatienceTime(double i) {
        this.patienceTime += i;
    }

    public Double getPatienceTime() {
        return patienceTime;
    }

    public void reset(double newFederateTime) {
        this.oldFederateTime = newFederateTime;
        queuePosition = -1;
        queueId = -1;
        hasServiceFinished = false;
    }
}
