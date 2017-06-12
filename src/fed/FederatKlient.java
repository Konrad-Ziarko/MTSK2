package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.Klient;

import java.util.Map.*;
import java.util.*;

public class FederatKlient extends AbstractFederat {
    private static final String federateName = "FederatKlient";


    private Random rand = new Random();
    private float generatingChance = .0f;
    private boolean shouldGenerateNewClient = false;

    private boolean shouldGeneratePrivileged = false;

    private List<Klient> allCustomers = new ArrayList<>();
    private Map<Integer, Integer> queuesSizes = new HashMap<>();
    private Map<Integer, Klient> customersHandlesToObjects = new HashMap<>();
    private Map<Klient, Integer> customersObjectsToHandles = new HashMap<>();


    public static void main(String[] args) {
        new FederatKlient().runFederate();
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
                double federateTime = fedamb.getFederateTime();
                if (rand.nextFloat() < generatingChance)
                    createAndRegisterCustomer(federateTime, false);
                if (shouldGenerateNewClient) {
                    createAndRegisterCustomer(fedamb.getFederateTime(), shouldGeneratePrivileged);
                    shouldGeneratePrivileged = shouldGenerateNewClient = false;
                }
                updateCustomersWithNewFederateTime(federateTime);
            }
            advanceTime(timeStep);

        }
    }

    private void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();
        fedamb.registerObjectInstanceCreatedListener((objectHandle, classHandle, objectName) -> {
            objectToClassHandleMap.put(objectHandle, classHandle);
            queuesSizes.put(objectHandle, 0);
        });
        fedamb.registerAttributesUpdatedListener((objectHandle, theAttributes, tag, theTime, retractionHandle) -> {
            if (queuesSizes.containsKey(objectHandle) && fedamb.kasaClassHandle.getClassHandle() == queuesSizes.get(objectHandle)) {
                int queueSize = -1;
                for (int i = 0; i < theAttributes.size(); i++) {
                    try {
                        byte[] value = theAttributes.getValue(i);
                        if (theAttributes.getAttributeHandle(i) == fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI)) {
                            queueSize = EncodingHelpers.decodeInt(value);
                        }
                    } catch (Exception e) {
                        log(e.getMessage());
                    }
                }
                queuesSizes.put(objectHandle, queueSize);
            }
        });
        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                log("Start interaction received");
                fedamb.setSimulationStarted(true);
            } else if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            } else if (interactionClass == fedamb.nowyKlientClassHandle.getClassHandle()) {
                log("Nowy klient interaction received");
                shouldGenerateNewClient = true;
                for (int i = 0; i < theInteraction.size(); i++) {
                    int attributeHandle = 0;
                    try {
                        attributeHandle = theInteraction.getParameterHandle(i);
                        String nameFor = fedamb.nowyKlientClassHandle.getNameFor(attributeHandle);
                        byte[] value = theInteraction.getValue(i);
                        if (nameFor.equalsIgnoreCase(UPRZYWILEJOWANY)) {
                            shouldGeneratePrivileged = EncodingHelpers.decodeBoolean(value);
                        }
                    } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                        arrayIndexOutOfBounds.printStackTrace();
                    }
                }
            } else if (interactionClass == fedamb.wejscieDoKasyClassHandle.getClassHandle()) {
                log("Customer has entered checkout");
                int nrKasy = -1;
                int nrKlienta = -1;
                for (int i = 0; i < theInteraction.size(); i++) {
                    int attributeHandle = 0;
                    try {
                        attributeHandle = theInteraction.getParameterHandle(i);
                        String nameFor = fedamb.wejscieDoKasyClassHandle.getNameFor(attributeHandle);
                        byte[] value = theInteraction.getValue(i);
                        if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                            nrKlienta = EncodingHelpers.decodeInt(value);
                        }
                    } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                        arrayIndexOutOfBounds.printStackTrace();
                    }
                }
                Klient tmp = customersHandlesToObjects.get(nrKlienta);
                customersObjectsToHandles.remove(tmp);
                customersHandlesToObjects.remove(nrKlienta);
            }
           /* else if (interactionClass == fedamb.obsluzonoKlientaClassHandle.getClassHandle()) {
                log("Customer has left bank");
                int nrKlienta = -1;
                for (int i = 0; i < theInteraction.size(); i++) {
                    int attributeHandle = 0;
                    try {
                        attributeHandle = theInteraction.getParameterHandle(i);
                        String nameFor = fedamb.obsluzonoKlientaClassHandle.getNameFor(attributeHandle);
                        byte[] value = theInteraction.getValue(i);
                        if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                            nrKlienta = EncodingHelpers.decodeInt(value);
                        }
                    } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                        arrayIndexOutOfBounds.printStackTrace();
                    }
                }
                try {
                    rtiamb.deleteObjectInstance(nrKlienta, generateTag());
                } catch (ObjectNotKnown | DeletePrivilegeNotHeld | FederateNotExecutionMember | SaveInProgress | RestoreInProgress | RTIinternalError | ConcurrentAccessAttempted objectNotKnown) {
                    objectNotKnown.printStackTrace();
                }
            }*/
        });
    }

    private Optional<Entry<Integer, Integer>> getShortestQueue() {
        Optional<Entry<Integer, Integer>> min = queuesSizes.entrySet().stream().min((entry1, entry2) -> {
            return entry1.getValue() - entry2.getValue();
        });
        return min;
    }

    private void updateCheckoutQueueWith(int objectHandle, byte[] value) throws ArrayIndexOutOfBounds {
        queuesSizes.put(objectHandle, EncodingHelpers.decodeInt(value));
    }

    private void updateCustomersWithNewFederateTime(double newFederateTime) {

        allCustomers.forEach(customer -> {
            customer.updateWithNewFederateTime(newFederateTime);
        });
        allCustomers.forEach(customer -> {
            submitNewTask(() -> {
                optionallySendQueueEnteredInteraction(customer, getShortestQueue());
            });
        });
        /*submitNewTask(() -> {
            Map<Klient, Integer> tmpList = customersObjectsToHandles.entrySet().stream().collect(Collectors.toMap(o -> o.getKey(), o -> o.getValue()));


            tmpList.forEach((Klient klient, Integer integer) -> {
                if (!klient.hasLeft && !klient.hasEntered && klient.wantsToLeaveQueue(newFederateTime)) {
                    log("Klient " + klient.getId() + " was impatient and left queue nr " + klient.getQueueId());
                    try {
                        SuppliedParameters parameters;
                        log(String.valueOf(fedamb.opuszczenieKolejkiClassHandle!=null) + ":");
                        parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                        parameters.add(fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KASY), EncodingHelpers.encodeInt(klient.getQueueId()));
                        parameters.add(fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(klient.getId()));
                        allCustomers.remove(klient);
                        for (int i = allCustomers.size() - 1; i >= 0; i--) {
                            if(allCustomers.get(i).getId() == klient.getId()){
                                allCustomers.remove(i);
                            }
                        }
                        klient.setHasLeft(true);
                        //queuesSizes.put(klient.getQueueId(), queuesSizes.get(klient.getQueueId()));

                        //Integer customerHandle = this.customersObjectsToHandles.get(klient);
                        this.customersObjectsToHandles.remove(klient);
                        this.customersHandlesToObjects.remove(integer);
                        //rtiamb.sendInteraction();  //wyslanie interakcji klient opuscil kolejke


                        rtiamb.sendInteraction(fedamb.opuszczenieKolejkiClassHandle.getClassHandle(), parameters, generateTag());
                        //rtiamb.deleteObjectInstance(integer, generateTag());
                    } catch (RTIinternalError rtIinternalError) {

                    } catch (SaveInProgress | RestoreInProgress | InteractionParameterNotDefined | FederateNotExecutionMember | ConcurrentAccessAttempted | InteractionClassNotPublished | InteractionClassNotDefined saveInProgress) {
                        log(saveInProgress.getMessage());
                    }

                }
            });
        });*/


    }

    private void optionallySendQueueEnteredInteraction(Klient customer, Optional<Entry<Integer, Integer>> min) {
        min.ifPresent(entry -> {
            log("Customer " + customersObjectsToHandles.get(customer) + " entering queue in checkout " + entry.getKey());
            entry.setValue(entry.getValue() + 1);
            customer.setQueueId(entry.getKey());
            sendQueueEnteredInteraction(customersObjectsToHandles.get(customer), entry.getKey(), customer.getNrSprawy(), customer.isPrivileged());
            this.allCustomers.remove(customer);
        });
    }

    private void sendQueueEnteredInteraction(Integer customerObjectId, Integer checkoutObjectId, Integer nrSprawy, boolean privileged) {
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KASY), EncodingHelpers.encodeInt(checkoutObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(customerObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_SPRAWY), EncodingHelpers.encodeInt(nrSprawy));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(UPRZYWILEJOWANY), EncodingHelpers.encodeBoolean(privileged));
            rtiamb.sendInteraction(fedamb.wejscieDoKolejkiClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send queue entered interaction, because: " + e.getMessage());
        }
    }

    private boolean isAttributeQueueLength(int attributeHandle, Class<?> typeFor) {
        return typeFor.getName().equalsIgnoreCase(Integer.class.getName()) && fedamb.kasaClassHandle.getNameFor(attributeHandle).equalsIgnoreCase(DLUGOSC_KOLEJKI);
    }

    private void createAndRegisterCustomer(double oldFederateTime, boolean isPrivileged) {
        Klient customer = new Klient(oldFederateTime, rand.nextInt(MAX_SERVICE_TIME - MIN_SERVICE_TIME + 1) + MIN_SERVICE_TIME);
        customer.setPrivileged(isPrivileged);
        //customer.patienceTime = rand.nextDouble() * (1000) + 800;
        allCustomers.add(customer);
        try {
            int customerHandle = registerRtiCustomer(customer);
            customer.setId(customerHandle);
            log("New customer " + customerHandle + " enters the bank: " + customer + " U=" + customer.isPrivileged());// + " |Patience = " + customer.patienceTime);
        } catch (RTIexception e) {
            log("Couldn't create new customer, because: " + e.getMessage());
        }
    }

    private int registerRtiCustomer(Klient customer) throws RTIexception {
        int customerHandle = rtiamb.registerObjectInstance(fedamb.klientClassHandle.getClassHandle());
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        attributes.add(fedamb.klientClassHandle.getHandleFor(UPRZYWILEJOWANY), EncodingHelpers.encodeBoolean(customer.isPrivileged()));
        attributes.add(fedamb.klientClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(customerHandle));

        rtiamb.updateAttributeValues(customerHandle, attributes, generateTag());
        customersHandlesToObjects.put(customerHandle, customer);
        customersObjectsToHandles.put(customer, customerHandle);
        return customerHandle;
    }

    public void publishAndSubscribe() {
        try {
            subscribeKasa();
            publishKlient();

            publishWejscieDoKolejki();
            publishOpuszczenieKolejki();

            subscribeWejscieDoKasy();
            subscribeNowyKlient();
            //subscribeObsluzonoKlienta();
            subscribeSimStart();
            subscribeSimStop();
        } catch
                (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | OwnershipAcquisitionPending | ObjectClassNotDefined | AttributeNotDefined
                        nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void deleteObjects() {
    }

    public void registerObjects() {
    }

}
