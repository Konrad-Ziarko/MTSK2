package shared;

import java.util.Comparator;

/**
 * Created by Konrad on 15.06.2017.
 */
public class ExternalTask implements Runnable {
    public Double runTime;
    public Runnable task;

    public ExternalTask(Runnable task, double runTime){
        this.task = task;
        this.runTime = runTime;
    }

    @Override
    public void run() {
        this.task.run();
    }

    public static class ExternalEventComparator implements Comparator<ExternalTask> {

        @Override
        public int compare(ExternalTask e1, ExternalTask e2) {
            return e1.runTime.compareTo(e2.runTime);
        }
    }
}
