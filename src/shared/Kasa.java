package shared;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by konrad on 5/29/17.
 */
public class Kasa {
    private Integer checkoutId;
    public boolean willBeClosed;
    public List<Klient> customersQueue;

    public Klient getCurrentCustomer() {
        return currentCustomer;
    }

    private Klient currentCustomer;

    public Kasa(int checkoutId) {
        this.willBeClosed = false;
        customersQueue = new LinkedList<>();
        this.checkoutId = checkoutId;
    }

    public void removeCustomerFromQueue(Klient k){
        customersQueue.remove(k);
        //customersQueue.removeIf(klient -> klient.getId()==k);
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

    public int getCheckoutId() {
        return checkoutId;
    }

    public void setCheckoutId(int checkoutId) {
        this.checkoutId = checkoutId;
    }
    public Klient getCurrentlyBuyingCustomer() {
        return currentCustomer;
    }

    public void updateCurrentBuyingCustomer(double federateTime, BiConsumer<Klient, Double> customerStartedBuyingAction) {
        if (currentCustomer == null && customersQueue.size() > 0 && !willBeClosed) {
            currentCustomer = customersQueue.remove(0);
            double queueWaitingTime = federateTime - currentCustomer.getOldFederateTime();
            currentCustomer.setOldFederateTime(federateTime);
            customerStartedBuyingAction.accept(currentCustomer, queueWaitingTime);
        }
        else if (willBeClosed && currentCustomer == null){
            //
        }
    }

    public void updateWithNewFederateTime(double federateTime, Consumer<Klient> customerFinishedBuyingAction) {
        if (currentCustomer != null) {
            currentCustomer.updateWithNewFederateTime(federateTime);
            if (currentCustomer.hasServiceFinished()) {
                customerFinishedBuyingAction.accept(currentCustomer);
                currentCustomer = null;
            }
        }
    }

    public void setWillBeClosed(boolean willBeClosed) {
        this.willBeClosed = willBeClosed;
    }
}
