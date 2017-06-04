package fed;

import amb.Ambasador;
import fom.Pair;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import javax.swing.*;
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
    private JTextArea textArea;
    private JScrollPane scrollPane;
    //

    private static final String CZAS_OBSLUGI = "sredniCzasObslugi";
    private static final String CZAS_CZEKANIA_NA_OBSLUGE = "sredniCzasOczekiwania";
    private static final String NR_OBSLUGIWANEGO = "nrObslugiwanegoKlienta";
    private static final String ID_KASA = "idKasa";
    private static final String ID_KLIENT = "idKlient";
    private static final String NR_KASY = "nrKasy";
    private static final String NR_W_KOLEJCE = "pozycjaKolejki";
    private static final String UPRZYWILEJOWANY = "czyUprzywilejowany";
    private static final String CZAS_ZAKUPOW = "rodzajZalatwianejSprawy";

    private boolean shouldProceed = false;
    private Ambasador fedAmbassador;
    private Map<Integer, Integer> checkoutObjectHandleToClassHandleMap;
    private Map<Integer, Integer> customerObjectHandleToClassHandleMap;
    private Map<Integer, Pair<Integer, Boolean>> checkoutObjectHandleToQueueSizeAndFilledMap;
    private List<Integer> customers;
    private Integer statisticsObjectHandle;
    private int customersLeft = 0;

    private boolean shouldSendStartInteraction = false;
    private boolean shouldSendStopInteraction = false;
    private boolean shouldSendCloseTheMarketInteraction = false;


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
        start = new JButton("Start");
        start.setEnabled(true);
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shouldSendStartInteraction = true;
            }
        });
        stop = new JButton("Stop");
        stop.setEnabled(true);
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                log("Sending \"stop\" interaction");
                SuppliedParameters parameters;
                try {
                    parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                    rtiamb.sendInteraction(fedamb.stopClassHandle.getClassHandle(), parameters, generateTag());
                } catch (RTIexception e1) {
                    log("Couldn't send \"stop\" interaction, because: " + e1.getMessage());
                }
            }
        });
        textArea = new JTextArea();
        textArea.setEnabled(false);

        JPanel panel = new JPanel();

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(textArea);
        scrollPane.setBounds(30, 65, 475, 180);
        panel.add(scrollPane);
        scrollPane.setSize(700, 400);
        scrollPane.setLocation(50, 100);

        panel.add(start);
        start.setSize(100, 30);
        start.setLocation(200, 20);
        panel.add(stop);
        stop.setSize(100, 30);
        stop.setLocation(400, 20);
        panel.add(scrollPane);
        frame.add(panel);
        frame.setContentPane(panel);
        panel.setLayout(null);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
    }

    public void runFederate() {
        createFederation();
        fedamb = prepareFederationAmbassador();
        joinFederation(federateName);
        registerSyncPoint();

        createWindow();

        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        while (fedamb.running) {
            if (shouldSendStartInteraction) {
                log("Sending \"start\" interaction");
                SuppliedParameters parameters;
                try {
                    parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                    rtiamb.sendInteraction(fedamb.startClassHandle.getClassHandle(), parameters, generateTag());
                } catch (RTIexception e1) {
                    log("Couldn't send \"start\" interaction, because: " + e1.getMessage());
                }
                shouldSendStartInteraction = false;
            }
            if (fedamb.isSimulationStarted()) {
                textArea.append("dupa\n");
                //log("dupa\n");
            }
            advanceTime(timeStep);

            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
        optionallySendStopFederationInteraction();
    }

    private void optionallySendStopFederationInteraction() {
        if (shouldSendStopInteraction) {
            log("Sending \"stop\" interaction");
            SuppliedParameters parameters;
            try {
                parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                rtiamb.sendInteraction(fedamb.stopClassHandle.getClassHandle(), parameters, generateTag());
            } catch (RTIexception e) {
                log("Couldn't send \"stop\" interaction, because: " + e.getMessage());
            }
            shouldSendStopInteraction = false;
        }
    }

    private void optionallySendCloseTheMarketInteraction() {
        if (shouldSendCloseTheMarketInteraction) {
            log("Sending \"close market\" interaction");
            SuppliedParameters parameters;
            try {
                parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                rtiamb.sendInteraction(fedamb.closeTheMarketClassHandle.getClassHandle(), parameters, generateTag());
            } catch (RTIexception e) {
                log("Couldn't send \"close the market\" interaction, because: " + e.getMessage());
            }
            shouldSendCloseTheMarketInteraction = false;
        }
    }

    public void cleanUpFederate() throws RTIexception {
        //controller.setFederationStatus("Cleaning up the federate");
        super.cleanUpFederate();
    }

   /* @Override
    protected AbstractFederat getFederateAmbassador() {
        return fedAmbassador;
    }*/

    protected Ambasador prepareFederationAmbassador() {
        fedAmbassador = new Ambasador();
        fedAmbassador.registerObjectInstanceCreatedListener((int theObject, int theObjectClass, String objectName) -> {
            if (theObjectClass == fedamb.klientClassHandle.getClassHandle()) {
                customerObjectHandleToClassHandleMap.put(theObject, theObjectClass);
                customers.add(theObject);
                log("Customer " + theObject + " entered, customers amount: " + customers.size());
            } else if (theObjectClass == fedamb.kasaClassHandle.getClassHandle()) {
                log("New cash opened " + theObject);
                checkoutObjectHandleToClassHandleMap.put(theObject, theObjectClass);
            } else if (theObjectClass == fedamb.statisticsClassHandle.getClassHandle()) {
                statisticsObjectHandle = theObject;
            }
        });
        fedAmbassador.registerObjectInstanceRemovedListener((int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                                             EventRetractionHandle retractionHandle) -> {
            if (customerObjectHandleToClassHandleMap.get(theObject) == fedamb.klientClassHandle.getClassHandle()) {
                customers.remove(new Integer(theObject));
                log("Customer " + theObject + " removed");
            }
        });
        fedAmbassador.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction,
                                                           byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.wejscieDoKolejkiClassHandle.getClassHandle()) {
                int extractCustomerClassHandle = extractCustomerClassHandle(theInteraction);
                log("Customer " + extractCustomerClassHandle + " entered queue");
                customers.remove(new Integer(extractCustomerClassHandle));
            } else if (interactionClass == fedamb.koniecObslugiClassHandle.getClassHandle()) {
                //controller.setCustomersLeft(++customersLeft);
            }
        });
        fedAmbassador.registerAttributesUpdatedListener((theObject, theAttributes, tag, theTime, whateverMan) -> {
            if (checkoutObjectHandleToClassHandleMap.containsKey(theObject)
                    && fedamb.kasaClassHandle.getClassHandle() == checkoutObjectHandleToClassHandleMap.get(theObject)) {
                handleCheckoutUpdate(theObject, theAttributes);
            } else if (statisticsObjectHandle != null && statisticsObjectHandle == theObject) {
                extractAndUpdateStatistics(theAttributes);
            }
        });
        return fedAmbassador;
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
        //controller.setAvgShoppingTime(avgShoppingTime);
        //controller.setAvgWaitingTime(avgWaitingTime);
        //controller.setAvgServiceTime(avgServiceTime);
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
        log("Cash " + theObject + " updated: queue size: " + queueSize + " filled");
        Pair<Integer, Boolean> value = new Pair<>(queueSize, filled);
        checkoutObjectHandleToQueueSizeAndFilledMap.put(theObject, value);
        //controller.updateCheckouts(theObject, value);
    }

    private int extractCustomerClassHandle(ReceivedInteraction theInteraction) {
        int handle = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == fedamb.wejscieDoKolejkiClassHandle.getHandleFor(ID_KLIENT)) {
                    handle = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                log(e.getMessage());
            }
        }
        return handle;
    }

    protected void publishAndSubscribe() {
        try {
            fedamb.kasaClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Kasa"),
                    new Pair<String, Class<?>>(NR_OBSLUGIWANEGO, Integer.class),
                    new Pair<String, Class<?>>(DLUGOSC_KOLEJKI, Integer.class));
            rtiamb.subscribeObjectClassAttributes(fedamb.kasaClassHandle.getClassHandle(),
                    fedamb.kasaClassHandle.createAttributeHandleSet());

            fedamb.klientClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Klient"),
                    new Pair<String, Class<?>>(NR_KLIENTA, Integer.class),
                    new Pair<String, Class<?>>(CZAS_ZAKUPOW, Integer.class),
                    new Pair<String, Class<?>>(UPRZYWILEJOWANY, Boolean.class),
                    new Pair<String, Class<?>>(NR_W_KOLEJCE, Integer.class),
                    new Pair<String, Class<?>>(NR_KASY, Integer.class));
            rtiamb.subscribeObjectClassAttributes(fedamb.klientClassHandle.getClassHandle(),
                    fedamb.klientClassHandle.createAttributeHandleSet());

            fedamb.statisticsClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Statystyka"),
                    new Pair<String, Class<?>>("sredniCzasObslugi", Double.class),
                    new Pair<String, Class<?>>("sredniCzasOczekiwania", Double.class),
                    new Pair<String, Class<?>>("sredniaDlugoscKolejki", Double.class));
            rtiamb.subscribeObjectClassAttributes(fedamb.statisticsClassHandle.getClassHandle(),
                    fedamb.statisticsClassHandle.createAttributeHandleSet());

            fedamb.wejscieDoKolejkiClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.wejscieDoKolejki"),
                    new Pair<String, Class<?>>(ID_KLIENT, Integer.class),
                    new Pair<String, Class<?>>(ID_KASA, Integer.class));
            rtiamb.subscribeInteractionClass(fedamb.wejscieDoKolejkiClassHandle.getClassHandle());

            fedamb.otworzKaseClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.otworzKase"));
            rtiamb.subscribeInteractionClass(fedamb.otworzKaseClassHandle.getClassHandle());

            fedamb.closeTheMarketClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamknijKase"));
            rtiamb.publishInteractionClass(fedamb.closeTheMarketClassHandle.getClassHandle());

            fedamb.stopClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_STOP_SIM));
            rtiamb.publishInteractionClass(fedamb.stopClassHandle.getClassHandle());

            fedamb.startClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_START_SIM));
            rtiamb.publishInteractionClass(fedamb.startClassHandle.getClassHandle());


        } catch (NameNotFound | FederateNotExecutionMember | SaveInProgress | RTIinternalError | ConcurrentAccessAttempted | ObjectClassNotDefined | RestoreInProgress | InteractionClassNotDefined | FederateLoggingServiceCalls | AttributeNotDefined nameNotFound) {
            nameNotFound.printStackTrace();
        }


    }

    protected void registerObjects() {

    }

    protected void deleteObjects() {

    }

    public void proceed() {
        this.shouldProceed = true;
    }

    public boolean shouldProceed() {
        return shouldProceed;
    }

    public void scheduleCloseTheMarketInteraction() {
        shouldSendCloseTheMarketInteraction = true;
    }

    public void sendStopInteraction() {
        shouldSendStopInteraction = true;
    }
}
