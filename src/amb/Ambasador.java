package amb;

import fom.FomInteraction;
import fom.FomObject;
import hla.rti.*;
import hla.rti.jlc.NullFederateAmbassador;
import org.portico.impl.hla13.types.DoubleTime;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by konrad on 5/28/17.
 */
public class Ambasador extends NullFederateAmbassador {
    public static final String FEDERATION_NAME = "BankFederation";
    public static final String READY_TO_RUN = "ReadyToRun";
    protected boolean simulationStarted = false;

    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    public void setSimulationStarted(boolean simulationStarted) {
        this.simulationStarted = simulationStarted;
    }


    public FomInteraction stopSymulacjiClassHandle;
    public FomInteraction startSymulacjiClassHandle;

    public FomInteraction nowyKlientClassHandle;

    public FomInteraction obsluzonoKlientaClassHandle;
    public FomInteraction wejscieDoKolejkiClassHandle;
    public FomInteraction opuszczenieKolejkiClassHandle;
    public FomInteraction wejscieDoKasyClassHandle;

    public FomInteraction potworzKaseClassHandle;
    public FomInteraction otworzKaseClassHandle;
    public FomInteraction zamknijKaseClassHandle;

    public FomObject statisticsClassHandle;
    public FomObject kasaClassHandle;
    public FomObject klientClassHandle;


    public double federateTime = 0.0;
    public double grantedTime = 0.0;
    public double federateLookahead = 1.0;

    public boolean isRegulating = false;
    public boolean isConstrained = false;
    public boolean isAdvancing = false;

    public boolean isAnnounced = false;
    public boolean isReadyToRun = false;


    public boolean shouldGenerateNewClient = false;
    public boolean shouldGeneratePrivileged = false;


    public boolean running = true;
    protected List<ObjectInstanceCreatedListener> objectInstanceCreatedListeners = new ArrayList<>();
    protected List<ObjectInstanceRemovedListener> objectInstanceRemovedListeners = new ArrayList<>();
    protected List<AttributesUpdatedListener> attributesUpdatedListeners = new ArrayList<>();
    protected List<InteractionReceivedListener> interactionReceivedListeners = new ArrayList<>();

    protected void log(String message) {
        System.out.println(message);
    }

    protected double convertTime(LogicalTime logicalTime) {
        return ((DoubleTime) logicalTime).getTime();
    }

    public void synchronizationPointRegistrationFailed(String label) {
        log("Failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {
        String intType = "";
        /*if (wejscieDoKolejkiClassHandle!=null && interactionClass == wejscieDoKolejkiClassHandle.getClassHandle())
            intType = "wejscie do kolejki";
        else if (obsluzonoKlientaClassHandle!=null &&interactionClass == obsluzonoKlientaClassHandle.getClassHandle())
            intType = "obsluzonoKlienta";
        else if (startSymulacjiClassHandle!=null &&interactionClass == startSymulacjiClassHandle.getClassHandle())
            intType = "startSymulacji";
        else if (stopSymulacjiClassHandle!=null &&interactionClass == stopSymulacjiClassHandle.getClassHandle())
            intType = "stopSymulacji";
        else if (nowyKlientClassHandle!=null &&interactionClass == nowyKlientClassHandle.getClassHandle())
            intType = "nowyKlient";
        else if (opuszczenieKolejkiClassHandle!=null &&interactionClass == opuszczenieKolejkiClassHandle.getClassHandle())
            intType = "opuszczenieKolejki";
        else if (wejscieDoKasyClassHandle!=null &&interactionClass == wejscieDoKasyClassHandle.getClassHandle())
            intType = "wejscieDoKasy";
        else if (otworzKaseClassHandle!=null &&interactionClass == otworzKaseClassHandle.getClassHandle())
            intType = "otworzKase";
        else if (zamknijKaseClassHandle!=null &&interactionClass == zamknijKaseClassHandle.getClassHandle())
            intType = "zamknijKase";
        */


        log("Received interaction of class " + intType + " id:"+ interactionClass);

        interactionReceivedListeners.forEach(listener -> {
            listener.notifyInteractionReceived(interactionClass, theInteraction, tag, theTime, eventRetractionHandle);
        });

    }

    public final void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(READY_TO_RUN))
            this.isAnnounced = true;
    }

    public void federationSynchronized(String label) {
        log("Federation Synchronized: " + label);
        if (label.equals(READY_TO_RUN))
            this.isReadyToRun = true;
    }

    public void removeObjectInstance(int theObject, byte[] userSuppliedTag) {
        removeObjectInstance(theObject, userSuppliedTag, null, null);
    }

    public void removeObjectInstance(int theObject, byte[] userSuppliedTag, LogicalTime theTime, EventRetractionHandle retractionHandle) {
        log("Object Removed: handle=" + theObject);
        objectInstanceRemovedListeners.forEach(listener -> {
            listener.objectInstanceRemoved(theObject, userSuppliedTag, theTime, retractionHandle);
        });
    }

    public double getFederateTime() {
        return federateTime;
    }

    public double getFederateLookahead() {
        return federateLookahead;
    }

    public void discoverObjectInstance(int theObject, int theObjectClass, String objectName) {
        log("New object of class " + theObjectClass + " created " + theObject + " " + objectName);
        objectInstanceCreatedListeners.forEach(listener -> {
            listener.notifyObjectInstanceDiscovery(theObject, theObjectClass, objectName);
        });
    }

    public void reflectAttributeValues(int theObject, ReflectedAttributes theAttributes, byte[] tag) {
        reflectAttributeValues(theObject, theAttributes, tag, null, null);
    }

    public void reflectAttributeValues(int theObject, ReflectedAttributes theAttributes, byte[] userSuppliedTag, LogicalTime theTime, EventRetractionHandle retractionHandle) {
        log("Attributes of object " + theObject + " changed");
        attributesUpdatedListeners.forEach(listener -> {
            Double time = null;
            if (theTime != null) {
                time = convertTime(theTime);
            }
            listener.notifyAttributesUpdated(theObject, theAttributes, userSuppliedTag, time, retractionHandle);
        });
    }

    public void registerObjectInstanceCreatedListener(ObjectInstanceCreatedListener listener) {
        objectInstanceCreatedListeners.add(listener);
    }

    public void registerAttributesUpdatedListener(AttributesUpdatedListener listener) {
        attributesUpdatedListeners.add(listener);
    }

    public void registerInteractionReceivedListener(InteractionReceivedListener listener) {
        interactionReceivedListeners.add(listener);
    }

    public void registerObjectInstanceRemovedListener(ObjectInstanceRemovedListener listener) {
        objectInstanceRemovedListeners.add(listener);
    }

    public interface ObjectInstanceRemovedListener {
        public void objectInstanceRemoved(int theObject, byte[] userSuppliedTag, LogicalTime theTime, EventRetractionHandle retractionHandle);
    }

    public interface ObjectInstanceCreatedListener {
        public void notifyObjectInstanceDiscovery(int theObject, int theObjectClass, String objectName);
    }

    public interface InteractionReceivedListener {
        public void notifyInteractionReceived(int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle);
    }

    public interface AttributesUpdatedListener {
        public void notifyAttributesUpdated(int theObject, ReflectedAttributes theAttributes, byte[] tag, Double time, EventRetractionHandle retractionHandle);
    }

    public void timeRegulationEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isConstrained = true;
    }

    public void timeAdvanceGrant(LogicalTime theTime) {
        this.federateTime = convertTime(theTime);
        this.isAdvancing = false;
    }

}
