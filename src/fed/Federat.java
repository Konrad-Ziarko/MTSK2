package fed;

import amb.Ambasador;
import amb.KlientA;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

/**
 * Created by konrad on 5/28/17.
 */
abstract class Federat {
    static final String READY_TO_RUN = "ReadyToRun";
    RTIambassador rtiamb;
    final double timeStep = 10.0;
    Ambasador fedamb;

    protected void log(String message) {
        System.out.println("KlientFederate   : " + message);
    }

    protected LogicalTimeInterval convertInterval(double time) {
        return new DoubleTimeInterval(time);
    }

    protected LogicalTime convertTime(double time) {
        return new DoubleTime(time);
    }

    protected void enableTimePolicy() throws RTIexception {
        LogicalTime currentTime = convertTime(fedamb.federateTime);
        LogicalTimeInterval lookahead = convertInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation(currentTime, lookahead);

        while (!fedamb.isRegulating) {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained) {
            rtiamb.tick();
        }
    }

    protected void advanceTime(double timeToAdvance) throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(timeToAdvance);
        rtiamb.timeAdvanceRequest(newTime);

        while (fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }
}
