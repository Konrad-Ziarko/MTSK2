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
    private Integer queueId;
    private Integer queuePosition;
    private Double oldFederateTime;
    private Double serviceTime;
    private Boolean hasServiceFinished;
    private Boolean privileged;
    private Integer nrSprawy;

    public Klient(double oldFederateTime, int serviceTime) {
        this.oldFederateTime = oldFederateTime;
        this.serviceTime = (double) serviceTime;
        privileged = false;
        queuePosition = -1;
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
}
