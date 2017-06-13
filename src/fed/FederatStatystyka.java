package fed;

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatStatystyka extends AbstractFederat {
    private static final String federateName = "FederateStatystyka";

    private List<Double> meanTimeInLine = new ArrayList<>();
    private List<Double> meanTimeInCheckout = new ArrayList<>();
    private List<Integer> meanCheckoutLineLength = new ArrayList<>();

    public static void main(String[] args) {
        new FederatStatystyka().runFederate();
    }


    protected void runFederate() {
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
            advanceTime(timeStep);
        }
        Integer sum = 0;
        for (Integer integer : meanCheckoutLineLength) {
            sum+=integer;
        }
        Double serviceSum = 0.0;
        for (Double aDouble : meanTimeInCheckout) {
            serviceSum+=aDouble;
        }
        Double lineSum = 0.0;
        for (Double aDouble : meanTimeInLine) {
            lineSum+=aDouble;
        }


        log("The average checkout queue length is " + (float)(sum/meanCheckoutLineLength.size()));
        log("The average time spent in queue is " + lineSum/meanTimeInLine.size());
        log("The average service time is "+ serviceSum/meanTimeInCheckout.size());

    }


    private void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();
        fedamb.registerObjectInstanceCreatedListener((objectHandle, classHandle, objectName) -> {
            meanCheckoutLineLength.add(0);
        });
        fedamb.registerAttributesUpdatedListener((objectHandle, theAttributes, tag, theTime, retractionHandle) -> {
            if (true){//fedamb.kasaClassHandle.getClassHandle() == queuesSizes.get(objectHandle)) {
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
                if(queueSize!=-1)
                    meanCheckoutLineLength.add(queueSize);
            }
        });

        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                log("Start interaction received");
                fedamb.setSimulationStarted(true);
            }
            if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            }
            if (interactionClass == fedamb.obsluzonoKlientaClassHandle.getClassHandle()) {
                double extractTime = extractBuyingTime(theInteraction);
                meanTimeInCheckout.add(extractTime);
            }
            if (interactionClass == fedamb.wejscieDoKasyClassHandle.getClassHandle()){
                Double waitingTime = -1.0;
                for (int i = 0; i < theInteraction.size(); i++) {
                    int attributeHandle = 0;
                    try {
                        attributeHandle = theInteraction.getParameterHandle(i);
                        String nameFor = fedamb.wejscieDoKasyClassHandle.getNameFor(attributeHandle);
                        byte[] value = theInteraction.getValue(i);
                        if (nameFor.equalsIgnoreCase(CZAS_CZEKANIA_NA_OBSLUGE)) {
                            waitingTime = EncodingHelpers.decodeDouble(value);
                        }
                    } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                        arrayIndexOutOfBounds.printStackTrace();
                    }
                }
                if(waitingTime!=-1.0)
                    meanTimeInLine.add(waitingTime);
            }
        });
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
            subscribeWejscieDoKasy();
            subscribeObsluzonoKlienta();
            subscribeWejscieDoKolejki();


            subscribeSimStop();
            subscribeSimStart();
        } catch
                (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | ObjectClassNotDefined | AttributeNotDefined
                        nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    protected void registerObjects() {

    }

    protected void deleteObjects() {

    }
}
