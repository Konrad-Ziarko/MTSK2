package fed;

import amb.Ambasador;
import fom.FomObjectDefinition;
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
    private Map<Integer, Klient> customersObjectsToHandles = new HashMap<>();

    public static void main(String[] args) {
        new FederatKasa().runFederate();
    }

    public void runFederate() {
        createFederation();
        prepareFederationAmbassador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForUser();
        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        System.out.println("\nRuszyli");
        while (fedamb.running) {
            if (fedamb.isSimulationStarted()) {
                executeAllQueuedTasks();
                checkoutObjectIdsToObjects.forEach((integer, checkout) -> {
                    double federateTime = fedamb.getFederateTime();
                    checkout.updateCurrentBuyingCustomer(federateTime, (buyingCustomer, waitingTime) -> {
                        log(federateTime + " " + buyingCustomer + " started being serviced after waiting " + waitingTime);
                        sendBuyingStartedInteraction(buyingCustomer.getId(), waitingTime, integer);
                        try {
                            updateCheckoutInRti(checkout.getCheckoutId(), checkout);
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    });
                    checkout.updateWithNewFederateTime(federateTime, finishedCustomer -> {
                        log(federateTime + " " + finishedCustomer + " finished being serviced after " + finishedCustomer.getServiceTime() + " time units");
                        sendBuyingFinishedInteraction(finishedCustomer);
                    });
                });
            }
            advanceTime(timeStep);
        }
    }

    private void sendBuyingStartedInteraction(Integer klient, Double waitingTime, Integer queueId) {
        log("Sending waiting time of " + waitingTime);
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.wejscieDoKasyClassHandle.getHandleFor(CZAS_CZEKANIA_NA_OBSLUGE), EncodingHelpers.encodeDouble(waitingTime));
            parameters.add(fedamb.wejscieDoKasyClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(klient));
            parameters.add(fedamb.wejscieDoKasyClassHandle.getHandleFor(NR_KASY), EncodingHelpers.encodeInt(queueId));
            rtiamb.sendInteraction(fedamb.wejscieDoKasyClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send service started interaction, because: " + e.getMessage());
        }
    }

    private void sendBuyingFinishedInteraction(Klient finishedCustomer) {
        double serviceTime = finishedCustomer.getServiceTime();
        log("Sending service finished with " + serviceTime);
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.obsluzonoKlientaClassHandle.getHandleFor(CZAS_OBSLUGI), EncodingHelpers.encodeDouble(serviceTime));
            parameters.add(fedamb.obsluzonoKlientaClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(finishedCustomer.getId()));
            customersObjectsToHandles.remove(finishedCustomer.getId());
            rtiamb.sendInteraction(fedamb.obsluzonoKlientaClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send service started interaction, because: " + e.getMessage());
        }
    }

    protected void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();


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
            } else if (interactionClass == fedamb.opuszczenieKolejkiClassHandle.getClassHandle()){
                submitNewTask(() -> {
                    Integer checkoutId = -1;
                    Integer customerId = -1;
                    for (int i = 0; i < theInteraction.size(); i++) {
                        try {
                            Integer attributeHandle = theInteraction.getParameterHandle(i);
                            String nameFor = fedamb.opuszczenieKolejkiClassHandle.getNameFor(attributeHandle);
                            byte[] value = theInteraction.getValue(i);
                            if (nameFor.equalsIgnoreCase(NR_KASY)) {
                                checkoutId = EncodingHelpers.decodeInt(value);
                            }  if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                                customerId = EncodingHelpers.decodeInt(value);
                            }
                        } catch (ArrayIndexOutOfBounds e) {
                            log(e.getMessage());
                        }
                    }
                    //final int nrK = customerId;
                    //customersObjectsToHandles.remove(finishedCustomer.getId());
                    final Klient k = customersObjectsToHandles.get(customerId);
                    //Kasa checkout = checkoutObjectIdsToObjects.get(checkoutId);
                    checkoutObjectIdsToObjects.forEach((integer, kasa) -> {
                        kasa.removeCustomerFromQueue(k);
                    });
                    customersObjectsToHandles.remove(customerId);
                    //checkout.removeCustomerFromQueue(customerId);
                    log("Usunięto klienta nr = " + customerId + " z kasy nr ");// + checkoutId);
                    updateCheckoutInRti(checkoutId, checkoutObjectIdsToObjects.get(checkoutId));
                });
            }
        });
    }

    private void initiateNewCheckout() {
        try {
            int checkoutObjectId = rtiamb.registerObjectInstance(fedamb.kasaClassHandle.getClassHandle());
            Kasa checkout = new Kasa(checkoutObjectId);
            checkoutObjectIdsToObjects.put(checkoutObjectId, checkout);
            updateCheckoutInRti(checkoutObjectId, checkout);
        } catch (Exception e) {
            log("Couldn't open new checkout, because: " + e.getMessage());
        }
    }

    private void updateCheckoutInRti(int checkoutId, Kasa checkout) {
        SuppliedAttributes attributes = null;
        try {
            attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
            byte[] encodedQueueSize = EncodingHelpers.encodeInt(checkout.getQueueSize());
            attributes.add(fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI), encodedQueueSize);
            rtiamb.updateAttributeValues(checkoutId, attributes, generateTag(), convertTime(fedamb.getFederateTime() + 1));
        } catch (InvalidFederationTime e) {
            log("Couldn't update Checkout " + checkoutId + ", because of invalid federation time error: " + e.getMessage());
        } catch (ObjectNotKnown | RTIinternalError | AttributeNotOwned | FederateNotExecutionMember | SaveInProgress | AttributeNotDefined | ConcurrentAccessAttempted | RestoreInProgress objectNotKnown) {
            objectNotKnown.printStackTrace();
        }
    }

    private void prepareCustomerAndUpdateCheckoutInRti(ReceivedInteraction theInteraction) {
        log("Received wejscieDoKolejki");
        try {
            Klient customer = new Klient(fedamb.getFederateTime(), 0,0);

            FomObjectDefinition<Integer, Integer> checkoutAndCustomerId = getCheckoutAndCustomerIdParameters(theInteraction, customer);
            Kasa checkout = checkoutObjectIdsToObjects.get(checkoutAndCustomerId.getT1());

            checkout.addCustomer(customer);
            customer.setServiceTime(rand.nextInt(MAX_SERVICE_TIME - MIN_SERVICE_TIME)*customer.getNrSprawy() + MIN_SERVICE_TIME);
            log("Customer " + customer.getId() + " entered queue in checkout " + checkoutAndCustomerId.getT1() + " with request id = " + customer.getNrSprawy() + " | service time = " + customer.getServiceTime());
            customersObjectsToHandles.put(customer.getId(), customer);
            updateCheckoutInRti(checkoutAndCustomerId.getT1(), checkout);
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private FomObjectDefinition<Integer, Integer> getCheckoutAndCustomerIdParameters(ReceivedInteraction theInteraction, Klient customer) throws ArrayIndexOutOfBounds {
        Integer checkoutId = -1;
        Integer customerId = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                Integer attributeHandle = theInteraction.getParameterHandle(i);
                String nameFor = fedamb.wejscieDoKolejkiClassHandle.getNameFor(attributeHandle);
                byte[] value = theInteraction.getValue(i);

                if (nameFor.equalsIgnoreCase(NR_KASY)) {
                    checkoutId = EncodingHelpers.decodeInt(value);
                }  if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                    customerId = EncodingHelpers.decodeInt(value);
                    customer.setId(customerId);
                } if (nameFor.equalsIgnoreCase(NR_SPRAWY)) {
                    customer.setNrSprawy(EncodingHelpers.decodeInt(value));
                }  if (nameFor.equalsIgnoreCase(UPRZYWILEJOWANY)) {
                    customer.setPrivileged(EncodingHelpers.decodeBoolean(value));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(e.getMessage());
            }

        }
        return new FomObjectDefinition<>(checkoutId, customerId);
    }

    public void publishAndSubscribe() {
        try {
            publishKasa();

            publishObsluzonoKlienta();
            publishWejscieDoKasy();

            subscribeKlient();

            subscribeOtworzKase();
            subscribeZamknijKase();
            subscribeOpuszczenieKolejki();
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
