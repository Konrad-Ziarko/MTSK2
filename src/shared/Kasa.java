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
    public List<Klient> customersQueue;
    private Klient buyingCustomer;

    public Kasa(int checkoutId) {
        customersQueue = new LinkedList<>();
        this.checkoutId = checkoutId;
    }

    public void removeCustomerFromQueue(Integer k){
        customersQueue.remove(k);
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
        return buyingCustomer;
    }

    public void updateCurrentBuyingCustomer(double federateTime, BiConsumer<Klient, Double> customerStartedBuyingAction) {
        if (buyingCustomer == null && customersQueue.size() > 0) {
            buyingCustomer = customersQueue.remove(0);
            double queueWaitingTime = federateTime - buyingCustomer.getOldFederateTime();
            buyingCustomer.setOldFederateTime(federateTime);
            customerStartedBuyingAction.accept(buyingCustomer, queueWaitingTime);
        }
    }

    public void updateWithNewFederateTime(double federateTime, Consumer<Klient> customerFinishedBuyingAction) {
        if (buyingCustomer != null) {
            buyingCustomer.updateWithNewFederateTime(federateTime);
            if (buyingCustomer.hasServiceFinished()) {
                customerFinishedBuyingAction.accept(buyingCustomer);
                buyingCustomer = null;
            }
        }
    }
}
