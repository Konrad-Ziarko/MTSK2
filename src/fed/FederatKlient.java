package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.Collections;

public class FederatKlient extends AbstractFederat {
    private int klientHandle;
    private static final String federateName = "KlientFederate";

    public static void main(String[] args) {
        new FederatKlient().runFederate();
    }

    public void runFederate() {
        createFederation();
        fedamb = new Ambasador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForSyncPoint();
        achieveSyncPoint();
        //timePolicy(); //może to miało być zrobione?
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            if (fedamb.externalEvents.size() > 0) {
                Collections.sort(fedamb.externalEvents, new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case ADD:
                            this.addToStock(externalEvent.getQty());
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }

            if (fedamb.grantedTime == timeToAdvance) {
                timeToAdvance += fedamb.federateLookahead;
                log("Updating stock at time: " + timeToAdvance);
                updateHLAObject(timeToAdvance);
                fedamb.federateTime = timeToAdvance;
            }
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
    }

    private void updateCheckoutQueueWith(int objectHandle, byte[] value) throws ArrayIndexOutOfBounds {
        checkoutsQueueSizes.put(objectHandle, EncodingHelpers.decodeInt(value));
    }

    protected void deleteObjects() throws RTIexception {
        log("Deleting " + customersHandlesToObjects.size() + " created customer objects");
        customersHandlesToObjects.keySet().stream().forEach(handle -> {
            try {
                rtiAmbassador.deleteObjectInstance(handle, generateTag());
            } catch (Exception e) {
                log("Couldn't delete " + handle + ", because: " + e.getMessage());
            }
        });
    }

    public String toString() {
        return "KlientFederate [bankCustomers[" + shoppingCustomers.size() + "], checkoutsQueueSizes["
                + checkoutsQueueSizes.size() + "]=" + checkoutsQueueSizes + "]";
    }

    private boolean isAttributeQueueLength(int attributeHandle, Class<?> typeFor) {
        return typeFor.getName().equalsIgnoreCase(Integer.class.getName())
                && kasaClassHandle.getNameFor(attributeHandle).equalsIgnoreCase(DLUGOSC_KOLEJKI);
    }

    public void publishAndSubscribe() {
        int classHandle = 0;
        try {
            classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Storage");
            int stockHandle = rtiamb.getAttributeHandle("stock", classHandle);
            AttributeHandleSet attributes =
                    RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
            attributes.add(stockHandle);

            rtiamb.publishObjectClass(classHandle, attributes);

            int addProductHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddProduct");
            fedamb.addProductHandle = addProductHandle;
            rtiamb.subscribeInteractionClass(addProductHandle);
        } catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | AttributeNotDefined | OwnershipAcquisitionPending | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | ObjectClassNotDefined nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void registerObjects() {
        int classHandle = 0;
        try {
            classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Klient");
            this.klientHandle = rtiamb.registerObjectInstance(classHandle);
        } catch (NameNotFound | FederateNotExecutionMember | SaveInProgress | RTIinternalError | ObjectClassNotDefined | ConcurrentAccessAttempted | ObjectClassNotPublished | RestoreInProgress nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void waitForSyncPoint() {

    }
}
