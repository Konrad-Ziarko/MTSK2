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
                double federateTime = getFederateAmbassador().getFederateTime();
                if (rand.nextFloat() < generatingChance)
                    createAndRegisterCustomer(federateTime, false);
                if (shouldGenerateNewClient) {
                    createAndRegisterCustomer(getFederateAmbassador().getFederateTime(), shouldGeneratePrivileged);
                    shouldGeneratePrivileged = shouldGenerateNewClient = false;
                }
                customersHandlesToObjects.values().forEach(customer -> {
                        if (customer.wantsToChangeQueue){
                            int handle = customersObjectsToHandles.get(customer);
                            sendCustomerLeftQueue(handle);
                            this.allCustomers.add(customer);
                            customersObjectsToHandles.remove(customer);
                            customersHandlesToObjects.remove(handle);
                        }
                });

                updateCustomersWithNewFederateTime(federateTime);


                //logika obslugi wszystkich klientow
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
            if (queuesSizes.containsKey(objectHandle)
                    && fedamb.kasaClassHandle.getClassHandle() == queuesSizes.get(objectHandle)) {
                int queueSize = -1;
                for (int i = 0; i < theAttributes.size(); i++) {
                    try {
                        byte[] value = theAttributes.getValue(i);
                        if (theAttributes.getAttributeHandle(i) == fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI)) {
                            queueSize = EncodingHelpers.decodeInt(value);
                        }
                    } catch (Exception e) {
                        log("blad2");
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
            }
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
           String tmp = customer.updateWithNewFederateTime(newFederateTime);
           if (tmp !=null && !tmp.equals("")){
               log(tmp);
           }
        });
        allCustomers.forEach(customer ->{
            submitNewTask(()->{
                optionallySendQueueEnteredInteraction(customer, getShortestQueue());
            });
        });
    }

    private void optionallySendQueueEnteredInteraction(Klient customer, Optional<Entry<Integer, Integer>> min) {
        min.ifPresent(entry -> {
            log("Customer " + customer + " entering queue in checkout " + entry.getKey());
            entry.setValue(entry.getValue() + 1);
            sendQueueEnteredInteraction(customersObjectsToHandles.get(customer), entry.getKey(), customer.isPrivileged());
            this.allCustomers.remove(customer);
        });
    }

    private void sendQueueEnteredInteraction(Integer customerObjectId, Integer checkoutObjectId, boolean privileged) {
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KLIENTA),
                    EncodingHelpers.encodeInt(customerObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KASY),
                    EncodingHelpers.encodeInt(checkoutObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(UPRZYWILEJOWANY),
                    EncodingHelpers.encodeBoolean(privileged));
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
        allCustomers.add(customer);
        try {
            int customerHandle = registerRtiCustomer(customer);
            log("New customer " + customerHandle + " enters the bank: " + customer + " U=" + customer.isPrivileged());
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

            subscribeNowyKlient();

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
