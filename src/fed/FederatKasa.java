package fed;

import amb.Ambasador;
import fom.Pair;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.Kasa;
import shared.Klient;

import java.util.*;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatKasa extends AbstractFederat {
    private static final String federateName = "KasaFederate";
    private static Random rand = new Random();


    private Map<Integer, Kasa> checkoutObjectIdsToObjects = new HashMap<>();


    public static void main (String[] args){
        new FederatKasa().runFederate();
    }



    public void runFederate() {
        createFederation();
        fedamb = prepareFederationAmbassador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForUser();
        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        System.out.println("\nRuszyli");
        while (fedamb.running) {
            if(fedamb.isSimulationStarted()){
                executeAllQueuedTasks();
                checkoutObjectIdsToObjects.values().forEach(checkout -> {
                    double federateTime = getFederateAmbassador().getFederateTime();
                    checkout.updateCurrentBuyingCustomer(federateTime, (buyingCustomer, waitingTime) -> {
                        log(federateTime+" "+buyingCustomer+" started being serviced after waiting "+ waitingTime);
                        sendBuyingStartedInteraction(waitingTime);
                        try {
                            updateCheckoutInRti(checkout.getCheckoutId(), checkout);
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    });
                    checkout.updateWithNewFederateTime(federateTime, finishedCustomer -> {
                        log(federateTime+" "+finishedCustomer+" finished being serviced after "+finishedCustomer.getServiceTime()+" time units");
                        sendBuyingFinishedInteraction(finishedCustomer.getServiceTime());
                    });
                });
            }
            advanceTime(timeStep);

        }
    }
    private void sendBuyingStartedInteraction(Double waitingTime) {
        log("Sending waiting time of "+ waitingTime);
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.wejscieDoKasyClassHandle.getHandleFor(CZAS_CZEKANIA_NA_OBSLUGE),
                    EncodingHelpers.encodeDouble(waitingTime));
            rtiamb.sendInteraction(fedamb.wejscieDoKasyClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send buying started interaction, because: "+ e.getMessage());
        }
    }
    private void sendBuyingFinishedInteraction(double buyingTime) {
        log("Sending buying time of "+ buyingTime);
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.obsluzonoKlientaClassHandle.getHandleFor(CZAS_OBSLUGI),
                    EncodingHelpers.encodeDouble(buyingTime));
            rtiamb.sendInteraction(fedamb.obsluzonoKlientaClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send buying started interaction, because: "+ e.getMessage());
        }
    }
    protected Ambasador prepareFederationAmbassador() {
        fedamb = new Ambasador();
        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
                    if (interactionClass == fedamb.otworzKaseClassHandle.getClassHandle()) {
                        submitNewTask(() -> {
                            log("Opening new checkout");
                            initiateNewCheckout();
                        });
                    } else if (interactionClass == fedamb.wejscieDoKolejkiClassHandle.getClassHandle()) {
                        submitNewTask(() -> {
                            prepareCustomerAndUpdateCheckoutInRti(theInteraction);
                        });
                    } else if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                        log("Start interaction received");
                        fedamb.setSimulationStarted(true);
                    } else if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                        log("Stop interaction received");
                        fedamb.running = false;
                    }
                });
        return fedamb;
    }

    private void initiateNewCheckout() {
        try {
            int checkoutObjectId = rtiamb.registerObjectInstance(fedamb.kasaClassHandle.getClassHandle());
            Kasa checkout = new Kasa(checkoutObjectId);
            checkoutObjectIdsToObjects.put(checkoutObjectId, checkout);
            updateCheckoutInRti(checkoutObjectId, checkout);
        } catch (Exception e) {
            log("Couldn't open new checkout, because: "+ e.getMessage());
        }
    }
    private void updateCheckoutInRti(int checkoutId, Kasa checkout){
        SuppliedAttributes attributes = null;
        try {
            attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
            byte[] encodedQueueSize = EncodingHelpers.encodeInt(checkout.getQueueSize());
            attributes.add(fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI), encodedQueueSize);
            rtiamb.updateAttributeValues(checkoutId, attributes, generateTag(), convertTime(fedamb.getFederateTime() + 1));
        } catch (InvalidFederationTime e) {
            log("Couldn't update Checkout "+checkoutId+", because of invalid federation time error: "+e.getMessage());
        } catch (ObjectNotKnown | RTIinternalError | AttributeNotOwned| FederateNotExecutionMember | SaveInProgress | AttributeNotDefined | ConcurrentAccessAttempted | RestoreInProgress objectNotKnown) {
            objectNotKnown.printStackTrace();
        }
    }

    private void prepareCustomerAndUpdateCheckoutInRti(ReceivedInteraction theInteraction) {
        log("Received wejscieDoKolejki");
        try {
            Klient customer = new Klient(getFederateAmbassador().getFederateTime(),rand.nextInt(MAX_SERVICE_TIME - MIN_SERVICE_TIME + 1) + MIN_SERVICE_TIME);
            Pair<Integer, Integer> checkoutAndCustomerId = getCheckoutAndCustomerIdParameters(theInteraction, customer);
            Kasa checkout = checkoutObjectIdsToObjects.get(checkoutAndCustomerId.getA());
            checkout.addCustomer(customer);
            log("Customer "+checkoutAndCustomerId.getB()+" entered queue in checkout "+checkoutAndCustomerId.getA());
            log(""+customer);
            updateCheckoutInRti(checkoutAndCustomerId.getA(), checkout);
        } catch (Exception e) {
            log(e.getMessage());
        }
    }
    private Pair<Integer, Integer> getCheckoutAndCustomerIdParameters(ReceivedInteraction theInteraction, Klient customer) throws ArrayIndexOutOfBounds {
        int checkoutId = -1;
        int customerId = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            int attributeHandle = theInteraction.getParameterHandle(i);
            String nameFor = fedamb.wejscieDoKolejkiClassHandle.getNameFor(attributeHandle);
            byte[] value = theInteraction.getValue(i);
            if (nameFor.equalsIgnoreCase(NR_KASY)) {
                checkoutId = EncodingHelpers.decodeInt(value);
            } else if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                customerId = EncodingHelpers.decodeInt(value);
            } else if (nameFor.equalsIgnoreCase(UPRZYWILEJOWANY)) {
                customer.setPrivileged(EncodingHelpers.decodeBoolean(value));
            }
        }
        return new Pair<>(checkoutId, customerId);
    }
    public void publishAndSubscribe(){
        try {

            publishKasa();
            subscribeKlient();

            subscribeOtworzKase();
            subscribeZamknijKase();

            publishObsluzonoKlienta();
            publishWejscieDoKasy();

            subscribeWejscieDoKolejki();

            subscribeSimStop();
            subscribeSimStart();
        } catch (NameNotFound | FederateNotExecutionMember | AttributeNotDefined | ObjectClassNotDefined | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | OwnershipAcquisitionPending nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void deleteObjects() {
    }
    public void registerObjects() {
    }
}
