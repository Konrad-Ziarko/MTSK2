package fed;

import amb.Ambasador;
import hla.rti.*;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatMenedzer extends AbstractFederat {
    private static final String federateName = "FederatMenedzer";

    public static void main(String[] args) {

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
    }

    private void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();

        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                log("Start interaction received");
                fedamb.setSimulationStarted(true);
            } else if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            }
        });
    }

    protected void publishAndSubscribe() {
        try {
            publishZamknijKase();
            publishOtworzKase();

            subscribeWejscieDoKasy();
            subscribeObsluzonoKlienta();

            subscribeWejscieDoKasy();
            subscribeNowyKlient();
            //subscribeObsluzonoKlienta();
            subscribeOtworzKase();
            subscribeZamknijKase();
            subscribeOpuszczenieKolejki();
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
