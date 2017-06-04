package shared;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by konrad on 6/3/17.
 */
public class Kolejka {
    private int objectId;
    private List<Klient> customersQueue;
    private Klient servicedCustomer;

    public Kolejka(int objectId) {
        this.objectId = objectId;
        customersQueue = new LinkedList<>();
    }

    public int getQueueSize() {
        return customersQueue.size();
    }

    public void addCustomer(Klient customer) {
        if (customer.isPrivileged()) {
            customersQueue.add(0, customer);
        } else {
            customersQueue.add(customer);
        }
    }

    public Klient getCurrentlyBuyingCustomer() {
        return servicedCustomer;
    }

    public int getObjectId() {
        return objectId;
    }

    public void updateCurrentBuyingCustomer(double federateTime, BiConsumer<Klient, Double> customerStartedBuyingAction) {
        if (servicedCustomer == null && customersQueue.size() > 0) {
            servicedCustomer = customersQueue.remove(0);
            double queueWaitingTime = federateTime - servicedCustomer.getOldFederateTime();
            servicedCustomer.setOldFederateTime(federateTime);
            customerStartedBuyingAction.accept(servicedCustomer, queueWaitingTime);
        }
    }

    public void updateWithNewFederateTime(double federateTime, Consumer<Klient> customerFinishedBuyingAction) {
        if (servicedCustomer != null) {
            servicedCustomer.updateWithNewFederateTime(federateTime);
            if (servicedCustomer.hasServiceFinished()) {
                customerFinishedBuyingAction.accept(servicedCustomer);
                servicedCustomer = null;
            }
        }
    }
}

