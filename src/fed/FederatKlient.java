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
    private static final String federateName = "FederateKlient";
    private static final double PATIENCE_TIME_INCEASE_MIN = 500;
    private static final double PATIENCE_TIME_INCEASE_MAX = 900;
    private static final double STARTING_PATIENCE_TIME_MIN = 600;
    private static final double STARTING_PATIENCE_TIME_MAX = 1200;

    private Random rand = new Random();
    private float generatingChance = .0f;
    private boolean shouldGenerateNewClient = false;

    private boolean shouldGeneratePrivileged = false;

    private List<Klient> waitingCustomers = new ArrayList<>();
    private List<Klient> inQueueCustomers = new ArrayList<>();

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
            } else if (interactionClass == fedamb.zamknijKaseClassHandle.getClassHandle()) {
                int dlKasy = Collections.min(queuesSizes.values());
                int nrKasy = -1;
                for (Integer integer : queuesSizes.keySet()) {
                    if(queuesSizes.get(integer) == dlKasy){
                        nrKasy = integer;
                        break;
                    }
                }
                log("Recived close checkout nr " + nrKasy + " interaction");
                queuesSizes.remove(nrKasy);
            }else if (interactionClass == fedamb.nowyKlientClassHandle.getClassHandle()) {
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
                        if (nameFor.equalsIgnoreCase(NR_KASY)) {
                            nrKasy = EncodingHelpers.decodeInt(value);
                        }
                    } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                        arrayIndexOutOfBounds.printStackTrace();
                    }
                }
                log("Customer "+nrKlienta+" has entered checkout "+nrKasy);

                queuesSizes.put(nrKasy, queuesSizes.get(nrKasy)-1);

                Klient tmp = customersHandlesToObjects.get(nrKlienta);
                inQueueCustomers.remove(tmp);
                customersObjectsToHandles.remove(tmp);
                customersHandlesToObjects.remove(nrKlienta);

                for (Klient inQueueCustomer : inQueueCustomers) {
                    int start=0;
                    inQueueCustomer.addToPatienceTime(rand.nextDouble()*(PATIENCE_TIME_INCEASE_MAX-PATIENCE_TIME_INCEASE_MIN)+PATIENCE_TIME_INCEASE_MIN);
                    if(inQueueCustomer.getQueueId() == nrKasy){
                        start = inQueueCustomer.getQueuePosition();
                        inQueueCustomer.setQueuePosition(inQueueCustomer.getQueuePosition()-1);
                    }
                    log("Customer "+inQueueCustomer.getId()+" has moved from " +start+ " to " + inQueueCustomer.getQueuePosition());

                }
            }
            else if (interactionClass == fedamb.obsluzonoKlientaClassHandle.getClassHandle()) {
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
                if (nrKlienta!=-1) {
                    log("Customer " + nrKlienta + " has left bank ?");
                    final int nrK = nrKlienta;
                    queuedTasks.add(() -> {
                        try {
                            rtiamb.deleteObjectInstance(nrK, generateTag());
                        } catch (ObjectNotKnown | DeletePrivilegeNotHeld | FederateNotExecutionMember | RestoreInProgress | SaveInProgress | ConcurrentAccessAttempted | RTIinternalError objectNotKnown) {
                            objectNotKnown.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    private Optional<Entry<Integer, Integer>> getShortestQueue() {
        Optional<Entry<Integer, Integer>> min = queuesSizes.entrySet().stream().min(Comparator.comparingInt(Entry::getValue));
        return min;
    }

    private void updateCheckoutQueueWith(int objectHandle, byte[] value) throws ArrayIndexOutOfBounds {
        queuesSizes.put(objectHandle, EncodingHelpers.decodeInt(value));
    }

    private void updateCustomersWithNewFederateTime(double newFederateTime) {

        waitingCustomers.forEach(customer -> {
            customer.updateWithNewFederateTime(newFederateTime);
        });
        waitingCustomers.forEach(customer -> {
            submitNewTask(() -> {
                optionallySendQueueEnteredInteraction(customer, getShortestQueue());
            });
        });
        submitNewTask(() -> {
            for (int i = inQueueCustomers.size() - 1; i >= 0; i--) {
                Klient tmp = inQueueCustomers.get(i);
                if(tmp!=null && tmp.wantsToLeave(newFederateTime) && tmp.getQueuePosition() > 1){
                    int minQueue = Collections.min(queuesSizes.values());
                    if (tmp.getQueuePosition() > minQueue){
                        log("Customer "+tmp.getId()+" was impatient and has left the queue ");

                        //queuesSizes.put(inQueueCustomer.getQueueId(), queuesSizes.get(inQueueCustomer.getQueueId())-1);

                        inQueueCustomers.remove(tmp);
                        //customersObjectsToHandles.remove(tmp); //tylko jesli go usuwam na amen
                        //customersHandlesToObjects.remove(tmp.getId());

                        SuppliedParameters parameters;
                        try {
                            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                            parameters.add(fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KASY), EncodingHelpers.encodeInt(tmp.getQueueId()));
                            parameters.add(fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KLIENTA), EncodingHelpers.encodeInt(tmp.getId()));
                            rtiamb.sendInteraction(fedamb.opuszczenieKolejkiClassHandle.getClassHandle(), parameters, generateTag());
                        /*try {
                            rtiamb.deleteObjectInstance(tmp.getId(), generateTag());
                        } catch (ObjectNotKnown | DeletePrivilegeNotHeld | FederateNotExecutionMember | RestoreInProgress | SaveInProgress | ConcurrentAccessAttempted | RTIinternalError objectNotKnown) {
                            objectNotKnown.printStackTrace();
                        }*/
                            tmp.reset(newFederateTime);
                            waitingCustomers.add(tmp);
                        } catch (RTIexception e) {
                            log("Couldn't send queue entered interaction, because: " + e.getMessage());
                        }
                    }


                }
            }
        });
    }

    private void optionallySendQueueEnteredInteraction(Klient customer, Optional<Entry<Integer, Integer>> min) {
        min.ifPresent(entry -> {

            entry.setValue(entry.getValue() + 1);
            customer.setQueueId(entry.getKey());
            customer.setQueuePosition(entry.getValue());
            log("Customer " + customersObjectsToHandles.get(customer) + " entering queue in checkout " + entry.getKey() + " on "+ customer.getQueuePosition()+" position");

            sendQueueEnteredInteraction(customersObjectsToHandles.get(customer), entry.getKey(), customer.getNrSprawy(), customer.isPrivileged());
            this.waitingCustomers.remove(customer);
            this.inQueueCustomers.add(customer);
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
        Klient customer = new Klient(oldFederateTime, rand.nextInt(MAX_SERVICE_TIME - MIN_SERVICE_TIME + 1) + MIN_SERVICE_TIME, rand.nextDouble()*(STARTING_PATIENCE_TIME_MAX-STARTING_PATIENCE_TIME_MIN)+STARTING_PATIENCE_TIME_MIN);
        customer.setPrivileged(isPrivileged);
        //customer.patienceTime = rand.nextDouble() * (1000) + 800;
        waitingCustomers.add(customer);
        try {
            int customerHandle = registerRtiCustomer(customer);
            customer.setId(customerHandle);
            log("New customer " + customerHandle + " enters the bank: " + customer + " U=" + customer.isPrivileged() + " |Patience = " + customer.getPatienceTime());
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

            subscribeZamknijKase();
            subscribeWejscieDoKasy();
            subscribeNowyKlient();
            subscribeObsluzonoKlienta();
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
