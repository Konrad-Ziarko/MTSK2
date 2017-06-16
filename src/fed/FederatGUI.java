package fed;

import amb.Ambasador;
import fom.FomObjectDefinition;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.ExternalTask;
import shared.Klient;
import shared.Zawartosc;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by konrad on 5/28/17.
 */
public class FederatGUI extends AbstractFederat {
    private static final String federateName = "FederateGUI";
    //GUI

    private JFrame frame;
    private JButton start;
    private JButton stop;
    private JButton newNormal;
    private JButton newPrivileged;
    private JButton newCheckout;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JTextArea textArea2;
    //
    private Zawartosc zawartosc;
    private Lock lock = new ReentrantLock();

    private Map<Integer, Integer> checkoutObjectHandleToClassHandleMap;
    private Map<Integer, Integer> customerObjectHandleToClassHandleMap;
    private Map<Integer, FomObjectDefinition<Integer, Boolean>> checkoutObjectHandleToQueueSizeAndFilledMap;
    private List<Integer> customers;
    private Integer statisticsObjectHandle;
    private int customersLeft = 0;

    private boolean shouldSendStartInteraction = false;
    private boolean shouldSendStopInteraction = false;
    private boolean shouldGenerateNewClient = false;
    private boolean shouldGeneratePrivileged = false;
    private boolean shouldGenerateNewCheckout = false;

    public void log(String str) {
        try {
            textArea.append(str + "\n");
        } catch (NullPointerException e) {
            System.out.println(str);
        }
    }

    public FederatGUI() {
        checkoutObjectHandleToClassHandleMap = new HashMap<>();
        customerObjectHandleToClassHandleMap = new HashMap<>();
        checkoutObjectHandleToQueueSizeAndFilledMap = new HashMap<>();
        customers = new LinkedList<>();
    }

    public static void main(String[] args) {
        new FederatGUI().runFederate();
    }

    private void createWindow() {
        frame = new JFrame(federateName);
        JPanel panel = new JPanel();

        start = new JButton("Start");
        start.setEnabled(true);
        start.addActionListener(e -> shouldSendStartInteraction = true);
        panel.add(start);
        start.setSize(100, 30);
        start.setLocation(50, 20);


        stop = new JButton("Stop");
        stop.setEnabled(false);
        stop.addActionListener(e -> shouldSendStopInteraction = true);
        panel.add(stop);
        stop.setSize(100, 30);
        stop.setLocation(200, 20);

        newNormal = new JButton("Normalny");
        newNormal.setEnabled(true);
        newNormal.addActionListener(e -> {
            shouldGenerateNewClient = true;
            shouldGeneratePrivileged = false;
        });
        panel.add(newNormal);
        newNormal.setSize(100, 30);
        newNormal.setLocation(350, 20);


        newPrivileged = new JButton("Uprzywilejowany");
        newPrivileged.setEnabled(true);
        newPrivileged.addActionListener(e -> {
            shouldGenerateNewClient = true;
            shouldGeneratePrivileged = true;
        });
        panel.add(newPrivileged);
        newPrivileged.setSize(100, 30);
        newPrivileged.setLocation(500, 20);


        newCheckout = new JButton("Nowa Kasa");
        newCheckout.setEnabled(true);
        newCheckout.addActionListener(e -> shouldGenerateNewCheckout = true);
        panel.add(newCheckout);
        newCheckout.setSize(100, 30);
        newCheckout.setLocation(650, 20);


        textArea = new JTextArea();
        textArea.setEnabled(true);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setBounds(30, 65, 475, 180);
        panel.add(scrollPane);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        scrollPane.setSize(700, 400);
        scrollPane.setLocation(50, 100);


        frame.add(panel);
        frame.setContentPane(panel);
        panel.setLayout(null);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JFrame frame2 = new JFrame("Wnetrze");
        JPanel panel2 = new JPanel();

        frame2.add(panel2);
        frame2.setContentPane(panel2);
        panel2.setLayout(null);
        frame2.pack();
        frame2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame2.setVisible(true);
        frame2.setSize(800, 600);
        frame2.setLocationRelativeTo(null);

        textArea2 = new JTextArea();
        textArea2.setEnabled(true);
        zawartosc = new Zawartosc(textArea2);
        JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setViewportView(textArea2);
        scrollPane2.setBounds(30, 65, 475, 180);
        panel2.add(scrollPane2);
        DefaultCaret caret2 = (DefaultCaret) textArea2.getCaret();
        caret2.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        scrollPane2.setSize(700, 400);
        scrollPane2.setLocation(50, 100);
    }

    private void generateNewClient(boolean b) {
        log("Sending \"nowy klient\" interaction");
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.nowyKlientClassHandle.getHandleFor(UPRZYWILEJOWANY),
                    EncodingHelpers.encodeBoolean(b));
            rtiamb.sendInteraction(fedamb.nowyKlientClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
        } catch (RTIexception e1) {
            log("Couldn't send \"nowy klient\" interaction, because: " + e1.getMessage());
        }
        shouldGenerateNewClient = shouldGeneratePrivileged = false;
    }

    private void generateNewCheckout() {
        log("Sending \"nowa kasa\" interaction");
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            rtiamb.sendInteraction(fedamb.potworzKaseClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
        } catch (RTIexception e1) {
            log("Couldn't send \"nowa kasa\" interaction, because: " + e1.getMessage());
        }
        shouldGenerateNewCheckout = false;
    }

    public void runFederate() {
        createWindow();
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
            executeAllQueuedTasks();
            if (shouldSendStartInteraction)
                sendStartInteraction();
            if (shouldSendStopInteraction)
                sendStopFederationInteraction();
            if (shouldGenerateNewClient)
                if (shouldGeneratePrivileged)
                    generateNewClient(true);
                else
                    generateNewClient(false);
            if (shouldGenerateNewCheckout)
                generateNewCheckout();

            executeAllQueuedTasks();
            advanceTime(timeStep);
            executeAllExternalTasks();
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
        sendStopFederationInteraction();
    }


    private void sendStartInteraction() {
        log("Sending \"start\" interaction");
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            rtiamb.sendInteraction(fedamb.startSymulacjiClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
            start.setEnabled(false);
            stop.setEnabled(true);
        } catch (RTIexception e1) {
            log("Couldn't send \"start\" interaction, because: " + e1.getMessage());
        }
        shouldSendStartInteraction = false;
    }

    private void sendStopFederationInteraction() {
        log("Sending \"stop\" interaction");
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            rtiamb.sendInteraction(fedamb.stopSymulacjiClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
            start.setEnabled(true);
            stop.setEnabled(false);
        } catch (RTIexception e) {
            log("Couldn't send \"stop\" interaction, because: " + e.getMessage());
        }
        shouldSendStopInteraction = false;
    }

    public void cleanUpFederate() throws RTIexception {
        super.cleanUpFederate();
    }


    protected void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();
        fedamb.registerObjectInstanceCreatedListener((int theObject, int theObjectClass, String objectName) -> {
            if (theObjectClass == fedamb.klientClassHandle.getClassHandle()) {
                customerObjectHandleToClassHandleMap.put(theObject, theObjectClass);
                customers.add(theObject);
                lock.lock();
                zawartosc.addCustomerToWaitingRoom(theObject);
                lock.unlock();
                log("Customer " + theObject + " entered, customers amount: " + customers.size());
            } else if (theObjectClass == fedamb.kasaClassHandle.getClassHandle()) {
                log("New checkout opened " + theObject + " prev number of checkouts = " + checkoutObjectHandleToClassHandleMap.size());
                lock.lock();
                zawartosc.addCheckout(theObject);
                lock.unlock();
                checkoutObjectHandleToClassHandleMap.put(theObject, theObjectClass);
            } else if (theObjectClass == fedamb.statisticsClassHandle.getClassHandle()) {
                log("New statistics object registered " + theObject);
                statisticsObjectHandle = theObject;
            }
        });
        fedamb.registerObjectInstanceRemovedListener((int theObject, byte[] userSuppliedTag, LogicalTime theTime, EventRetractionHandle retractionHandle) -> {
            externalTasks.add(new ExternalTask(()->{
                if (customerObjectHandleToClassHandleMap.get(theObject) == fedamb.klientClassHandle.getClassHandle()) {
                    customers.remove(new Integer(theObject));
                    log("Customer " + theObject + " left bank");
                    lock.lock();
                    zawartosc.removeCustomer(theObject);
                    lock.unlock();
                }
            }, convertTime(theTime)));

        });
        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            externalTasks.add(new ExternalTask(()->{
                if (interactionClass == fedamb.wejscieDoKolejkiClassHandle.getClassHandle()) {
                    reciveQueueEntered(theInteraction);
                }
                if (interactionClass == fedamb.obsluzonoKlientaClassHandle.getClassHandle()) {
                    double extractTime = extractBuyingTime(theInteraction);
                    int extractCustomer = extractCustomerId(theInteraction);
                    lock.lock();
                    zawartosc.removeCustomerFromCheckout(extractCustomer);
                    lock.unlock();
                    log("Customer " + extractCustomer + " left checkout after " + extractTime + " time");
                }
                if (interactionClass == fedamb.wejscieDoKasyClassHandle.getClassHandle()) {
                    int nrKasy = -1;
                    int nrKlienta = -1;
                    for (int i = 0; i < theInteraction.size(); i++) {
                        int attributeHandle;
                        try {
                            attributeHandle = theInteraction.getParameterHandle(i);
                            String nameFor = fedamb.wejscieDoKasyClassHandle.getNameFor(attributeHandle);
                            byte[] value = theInteraction.getValue(i);
                            if (nameFor.equalsIgnoreCase(NR_KASY)) {
                                nrKasy = EncodingHelpers.decodeInt(value);
                            }
                            if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                                nrKlienta = EncodingHelpers.decodeInt(value);
                            }
                        } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                            arrayIndexOutOfBounds.printStackTrace();
                        }
                    }
                    lock.lock();
                    zawartosc.customerIsBeingServiced(nrKlienta, nrKasy);
                    lock.unlock();
                    log("Customer " + nrKlienta + " entered checkout " + nrKasy);
                }
                if (interactionClass == fedamb.otworzKaseClassHandle.getClassHandle()) {
                    log("Recived open new checkout interaction");
                    //
                }
                if (interactionClass == fedamb.zamknijKaseClassHandle.getClassHandle()) {
                    int nrKasy = -1;
                    for (int i = 0; i < theInteraction.size(); i++) {
                        int attributeHandle;
                        try {
                            attributeHandle = theInteraction.getParameterHandle(i);
                            String nameFor = fedamb.zamknijKaseClassHandle.getNameFor(attributeHandle);
                            byte[] value = theInteraction.getValue(i);
                            if (nameFor.equalsIgnoreCase(NR_KASY)) {
                                nrKasy = EncodingHelpers.decodeInt(value);
                            }
                        } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                            arrayIndexOutOfBounds.printStackTrace();
                        }
                    }
                    lock.lock();
                    zawartosc.removeCheckout(nrKasy);
                    lock.unlock();
                    log("Recived close checkout nr "+nrKasy+" interaction");
                }

                if (interactionClass == fedamb.opuszczenieKolejkiClassHandle.getClassHandle()) {

                    Integer checkoutId = -1;
                    Integer customerId = -1;
                    for (int i = 0; i < theInteraction.size(); i++) {
                        try {
                            Integer attributeHandle = theInteraction.getParameterHandle(i);
                            String nameFor = fedamb.opuszczenieKolejkiClassHandle.getNameFor(attributeHandle);
                            byte[] value = theInteraction.getValue(i);
                            if (nameFor.equalsIgnoreCase(NR_KASY)) {
                                checkoutId = EncodingHelpers.decodeInt(value);
                            }
                            if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                                customerId = EncodingHelpers.decodeInt(value);
                            }
                        } catch (ArrayIndexOutOfBounds e) {
                            log(3+""+e.getMessage());
                        }
                    }
                    lock.lock();
                    zawartosc.removeCustomerFromQueue(customerId, checkoutId);
                    lock.unlock();
                    log("Customer " + customerId + " has left the queue <tmp left bank> because was waiting too long in checkout " + checkoutId);

                }
            }, convertTime(theTime)));

        });
        fedamb.registerAttributesUpdatedListener((int theObject, ReflectedAttributes theAttributes, byte[] tag, Double time, EventRetractionHandle retractionHandle) -> {
            externalTasks.add(new ExternalTask(()->{
                if (checkoutObjectHandleToClassHandleMap.containsKey(theObject) && fedamb.kasaClassHandle.getClassHandle() == checkoutObjectHandleToClassHandleMap.get(theObject)) {
                    handleCheckoutUpdate(theObject, theAttributes);
                } else if (statisticsObjectHandle != null && statisticsObjectHandle == theObject) {
                    extractAndUpdateStatistics(theAttributes);
                }
            }, time));

        });
    }

    private void reciveQueueEntered(ReceivedInteraction theInteraction) {
        try {
            Klient customer = new Klient(fedamb.getFederateTime(), 0, 0);
            FomObjectDefinition<Integer, Integer> checkoutAndCustomerId = getCheckoutAndCustomerIdParameters(theInteraction, customer);

            log("Customer " + customer.getId() + " entered queue in checkout " + checkoutAndCustomerId.getT1() + " with request id = " + customer.getNrSprawy());
            lock.lock();
            zawartosc.removeCustomerFromWaitingRoom(customer.getId());
            lock.unlock();
            if (customer.isPrivileged()){
                lock.lock();
                zawartosc.addPriviligedCustomerToCheckout(customer.getId(), checkoutAndCustomerId.getT1());
                lock.unlock();
            }else {
                lock.lock();
                zawartosc.addCustomerToCheckout(customer.getId(), checkoutAndCustomerId.getT1());
                lock.unlock();
            }
            customers.remove(customer.getId());

        } catch (Exception e) {
            log(4+""+e.getMessage());
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
                }
                if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                    customerId = EncodingHelpers.decodeInt(value);
                    customer.setId(customerId);
                }
                if (nameFor.equalsIgnoreCase(NR_SPRAWY)) {
                    customer.setNrSprawy(EncodingHelpers.decodeInt(value));
                }
                if (nameFor.equalsIgnoreCase(UPRZYWILEJOWANY)) {
                    customer.setPrivileged(EncodingHelpers.decodeBoolean(value));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(5+""+e.getMessage());
            }

        }
        return new FomObjectDefinition<>(checkoutId, customerId);
    }

    private void extractAndUpdateStatistics(ReflectedAttributes theAttributes) {
        double avgShoppingTime = -1;
        double avgWaitingTime = -1;
        double avgServiceTime = -1;
        for (int i = 0; i < theAttributes.size(); i++) {
            try {
                byte[] value = theAttributes.getValue(i);
                if (theAttributes.getAttributeHandle(i) == fedamb.statisticsClassHandle.getHandleFor("avgShoppingTime")) {
                    avgShoppingTime = EncodingHelpers.decodeDouble(value);
                } else if (theAttributes.getAttributeHandle(i) == fedamb.statisticsClassHandle.getHandleFor("avgWaitingTime")) {
                    avgWaitingTime = EncodingHelpers.decodeDouble(value);
                } else if (theAttributes.getAttributeHandle(i) == fedamb.statisticsClassHandle
                        .getHandleFor("avgServiceTime")) {
                    avgServiceTime = EncodingHelpers.decodeDouble(value);
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(6+""+e.getMessage());
            }
        }
        updateStatistics(avgShoppingTime, avgWaitingTime, avgServiceTime);
    }

    private void updateStatistics(double avgShoppingTime, double avgWaitingTime, double avgServiceTime) {
    }

    private void handleCheckoutUpdate(int theObject, ReflectedAttributes theAttributes) {
        int queueSize = -1;
        boolean filled = false;
        for (int i = 0; i < theAttributes.size(); i++) {
            try {
                byte[] value = theAttributes.getValue(i);
                if (theAttributes.getAttributeHandle(i) == fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI)) {
                    queueSize = EncodingHelpers.decodeInt(value);
                }
            } catch (Exception e) {
                log(7+""+e.getMessage());
            }
        }
        log("Checkout " + theObject + " updated: queue size: " + queueSize + " filled");
        FomObjectDefinition<Integer, Boolean> value = new FomObjectDefinition<>(queueSize, filled);
        checkoutObjectHandleToQueueSizeAndFilledMap.put(theObject, value);
    }

    private Integer extractNrSprawy(ReceivedInteraction theInteraction) {
        Integer handle = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_SPRAWY)) {
                    handle = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(7+""+e.getMessage());
            }
        }
        return handle;
    }

    private boolean extractPrivileged(ReceivedInteraction theInteraction) {
        boolean handle = false;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == fedamb.wejscieDoKolejkiClassHandle.getHandleFor(UPRZYWILEJOWANY)) {
                    handle = EncodingHelpers.decodeBoolean(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(8+""+e.getMessage());
            }
        }
        return handle;
    }

    private int extractClassHandle(ReceivedInteraction theInteraction) {
        int handle = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KLIENTA)) {
                    handle = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(9+""+e.getMessage());
            }
        }
        return handle;
    }

    private int extractCustomerId(ReceivedInteraction theInteraction) {
        int retrivedId = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                int attributeHandle = theInteraction.getParameterHandle(i);
                String nameFor = fedamb.obsluzonoKlientaClassHandle.getNameFor(attributeHandle);
                if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                    retrivedId = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (Exception e) {
                log(10+""+e.getMessage());
            }
        }
        return retrivedId;
    }

    private double extractBuyingTime(ReceivedInteraction theInteraction) {
        double buyingTime = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                int attributeHandle = theInteraction.getParameterHandle(i);
                String nameFor = fedamb.obsluzonoKlientaClassHandle.getNameFor(attributeHandle);
                if (nameFor.equalsIgnoreCase(CZAS_OBSLUGI)) {
                    buyingTime = EncodingHelpers.decodeDouble(theInteraction.getValue(i));
                }
            } catch (Exception e) {
                log(11+""+e.getMessage());
            }
        }
        return buyingTime;
    }

    protected void publishAndSubscribe() {
        try {
            subscribeKasa();
            subscribeKlient();
            subscribeStatystyka();
            subscribeWejscieDoKasy();
            subscribeWejscieDoKolejki();
            subscribeObsluzonoKlienta();
            subscribeOpuszczenieKolejki();
            subscribeZamknijKase();

            subscribeOtworzKase();
            publishOtworzKase();

            publishNowyKlient();
            publishSimStart();
            publishSimStop();
        } catch (NameNotFound | FederateNotExecutionMember | SaveInProgress | RTIinternalError | ConcurrentAccessAttempted | ObjectClassNotDefined | RestoreInProgress | InteractionClassNotDefined | FederateLoggingServiceCalls | AttributeNotDefined nameNotFound) {
            log("Name not found");
            nameNotFound.printStackTrace();
        }
    }

    protected void registerObjects() {

    }

    protected void deleteObjects() {

    }
}
