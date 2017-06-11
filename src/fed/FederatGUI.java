package fed;

import amb.Ambasador;
import fom.FomObjectDefinition;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by konrad on 5/28/17.
 */
public class FederatGUI extends AbstractFederat {
    private static final String federateName = "FederatGUI";
    //GUI

    private JFrame frame;
    private JButton start;
    private JButton stop;
    private JButton newNormal;
    private JButton newPrivileged;
    private JButton newCheckout;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    //

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
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldSendStartInteraction = true;
            }
        });
        panel.add(start);
        start.setSize(100, 30);
        start.setLocation(50, 20);


        stop = new JButton("Stop");
        stop.setEnabled(false);
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldSendStopInteraction = true;
            }
        });
        panel.add(stop);
        stop.setSize(100, 30);
        stop.setLocation(200, 20);

        newNormal = new JButton("Normalny");
        newNormal.setEnabled(true);
        newNormal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldGenerateNewClient = true;
                shouldGeneratePrivileged = false;
            }
        });
        panel.add(newNormal);
        newNormal.setSize(100, 30);
        newNormal.setLocation(350, 20);


        newPrivileged = new JButton("Uprzywilejowany");
        newPrivileged.setEnabled(true);
        newPrivileged.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldGenerateNewClient = true;
                shouldGeneratePrivileged = true;
            }
        });
        panel.add(newPrivileged);
        newPrivileged.setSize(100, 30);
        newPrivileged.setLocation(500, 20);


        newCheckout = new JButton("Nowa Kasa");
        newCheckout.setEnabled(true);
        newCheckout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldGenerateNewCheckout = true;
            }
        });
        panel.add(newCheckout);
        newCheckout.setSize(100, 30);
        newCheckout.setLocation(650, 20);


        textArea = new JTextArea();
        textArea.setEnabled(true);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setBounds(30, 65, 475, 180);
        panel.add(scrollPane);
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
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
    }

    private void generateNewClient(boolean b) {
        log("Sending \"nowy klient\" interaction");
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.nowyKlientClassHandle.getHandleFor(UPRZYWILEJOWANY),
                    EncodingHelpers.encodeBoolean(b));
            rtiamb.sendInteraction(fedamb.nowyKlientClassHandle.getClassHandle(), parameters, generateTag());
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
            rtiamb.sendInteraction(fedamb.otworzKaseClassHandle.getClassHandle(), parameters, generateTag());
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
            if(shouldGenerateNewCheckout)
                generateNewCheckout();

            advanceTime(timeStep);
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
            rtiamb.sendInteraction(fedamb.startSymulacjiClassHandle.getClassHandle(), parameters, generateTag());
            //start.setEnabled(false);
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
            rtiamb.sendInteraction(fedamb.stopSymulacjiClassHandle.getClassHandle(), parameters, generateTag());
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
                log("Customer " + theObject + " entered, customers amount: " + customers.size());
                customerObjectHandleToClassHandleMap.put(theObject, theObjectClass);
                customers.add(theObject);
            } else if (theObjectClass == fedamb.kasaClassHandle.getClassHandle()) {
                log("New checkout opened " + theObject);
                checkoutObjectHandleToClassHandleMap.put(theObject, theObjectClass);
            } else if (theObjectClass == fedamb.statisticsClassHandle.getClassHandle()) {
                log("New statistics object registered " + theObject);
                statisticsObjectHandle = theObject;
            }
        });
        fedamb.registerObjectInstanceRemovedListener((int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                                      EventRetractionHandle retractionHandle) -> {
            if (customerObjectHandleToClassHandleMap.get(theObject) == fedamb.klientClassHandle.getClassHandle()) {
                customers.remove(new Integer(theObject));
                log("Customer " + theObject + " removed");
            }
        });
        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.wejscieDoKolejkiClassHandle.getClassHandle()) {
                int extractCustomerClassHandle = extractClassHandle(theInteraction);
                boolean isPrivileged = extractPrivileged(theInteraction);
                log("Customer " + extractCustomerClassHandle + " entered queue |U=" + isPrivileged);
                customers.remove(new Integer(extractCustomerClassHandle));
            }
            if (interactionClass == fedamb.obsluzonoKlientaClassHandle.getClassHandle()) {
                double extractTime = extractBuyingTime(theInteraction);
                int extractCustomer = extractCustomerId(theInteraction);
                log("Customer "+extractCustomer+" left checkout after " + extractTime + " time");
            }
            if (interactionClass == fedamb.opuszczenieKolejkiClassHandle.getClassHandle()) {
                int customerId = -1;
                for (int i = 0; i < theInteraction.size(); i++) {
                    try {
                            /*if (theInteraction.getParameterHandle(i) == fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KASY)) {
                                checkoutId = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                            } else*/
                        if (theInteraction.getParameterHandle(i) == fedamb.opuszczenieKolejkiClassHandle.getHandleFor(NR_KLIENTA)) {
                            customerId = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                        }
                    } catch (ArrayIndexOutOfBounds e) {
                        log(e.getMessage());
                    }
                }
                log("Customer " + customerId + " left queue");
            }
        });
        fedamb.registerAttributesUpdatedListener((theObject, theAttributes, tag, theTime, whateverMan) -> {
            if (checkoutObjectHandleToClassHandleMap.containsKey(theObject) && fedamb.kasaClassHandle.getClassHandle() == checkoutObjectHandleToClassHandleMap.get(theObject)) {
                handleCheckoutUpdate(theObject, theAttributes);
            } else if (statisticsObjectHandle != null && statisticsObjectHandle == theObject) {
                extractAndUpdateStatistics(theAttributes);
            }
        });
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
                log(e.getMessage());
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
                log(e.getMessage());
            }
        }
        log("Checkout " + theObject + " updated: queue size: " + queueSize + " filled");
        FomObjectDefinition<Integer, Boolean> value = new FomObjectDefinition<>(queueSize, filled);
        checkoutObjectHandleToQueueSizeAndFilledMap.put(theObject, value);
    }
    private boolean extractPrivileged(ReceivedInteraction theInteraction) {
        boolean handle = false;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == fedamb.wejscieDoKolejkiClassHandle.getHandleFor(UPRZYWILEJOWANY)) {
                    handle = EncodingHelpers.decodeBoolean(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(e.getMessage());
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
                log(e.getMessage());
            }
        }
        return handle;
    }
    private int extractCustomerId(ReceivedInteraction theInteraction){
        int retrivedId = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                int attributeHandle = theInteraction.getParameterHandle(i);
                String nameFor = fedamb.obsluzonoKlientaClassHandle.getNameFor(attributeHandle);
                if (nameFor.equalsIgnoreCase(NR_KLIENTA)) {
                    retrivedId = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (Exception e) {
                log(e.getMessage());
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
                log(e.getMessage());
            }
        }
        return buyingTime;
    }

    protected void publishAndSubscribe() {
        try {
            subscribeKasa();
            subscribeKlient();
            subscribeStatystyka();

            subscribeOpuszczenieKolejki();
            subscribeWejscieDoKolejki();
            subscribeObsluzonoKlienta();

            subscribeWejscieDoKasy();

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
