package shared;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Konrad on 16.06.2017.
 */
public class Zawartosc {
    private JTextArea area;

    public Zawartosc(JTextArea area){
        this.area = area;
    }

    private String string = "";
    public List<String> historia = new ArrayList<>();
    public List<Integer> waitingRoom = new ArrayList<>();
    public Map<Integer, Integer> checkouts = new HashMap<>();
    public Map<Integer, List<Integer>> queues = new HashMap<>();
    private int nrKasy;

    public void clearString(){
        string="";
    }
    public void appendToString(String str){
        string +=str;
    }

    public void addToTextArea(){
        String str = toString();
        area.setText(str);
        historia.add(str);
    }

    public void addCustomerToWaitingRoom(int customer){
        waitingRoom.add(customer);
        addToTextArea();
    }

    public void addCheckout(int checkout){
        checkouts.put(checkout, -1);
        queues.put(checkout, new ArrayList<>());
        addToTextArea();
    }

    public void addCustomerToCheckout(int customer, int checkout){
        queues.get(checkout).add(customer);
        addToTextArea();
    }

    public void addPriviligedCustomerToCheckout(int customer, int checkout){
        queues.get(checkout).add(0, customer);
        addToTextArea();
    }

    public void removeCustomerFromQueue(int customer, int checkout){
        queues.get(checkout).remove((Object)customer) ;
        addToTextArea();
    }

    public void removeCustomerFromCheckout(int nrKlienta){
        for (Integer integer : queues.keySet()) {
            queues.get(integer).remove((Object)nrKlienta);
        }
        for (Integer integer : checkouts.keySet()) {
            if(checkouts.get(integer)==nrKlienta)
                checkouts.put(integer, -1);
        }
        addToTextArea();
    }

    public void setNrKasy(int nrKasy){
        this.nrKasy = nrKasy;
        addToTextArea();
    }

    public void removeCustomer(int theObject) {
        checkouts.forEach((integer, integer2) -> {
            if (integer2==theObject){
                setNrKasy(integer);
            }
        });
        checkouts.put(nrKasy, -1);
        addToTextArea();
    }

    public void removeCustomerFromWaitingRoom(Integer id) {
        waitingRoom.remove(id);
        addToTextArea();
    }

    public void customerIsBeingServiced(int nrKlienta, int nrKasy) {
        //queues.get(nrKasy).remove(nrKlienta);
        checkouts.put(nrKasy, nrKlienta);
        addToTextArea();
    }

    public void removeCheckout(int nrKasy) {
        checkouts.remove(nrKasy);
        addToTextArea();
    }

    public String toString(){
        clearString();
        appendToString("Wejscie:");
        for (Integer integer : waitingRoom) {
            appendToString("/"+integer+"/  ");
        }
        appendToString("\n");



        checkouts.forEach((integer, integer2) -> {
            if(integer!=0) {
                appendToString("" + integer + ":");
                try {
                    for (Integer integer4 : queues.get(integer)) {
                        appendToString("/" + integer4 + "/  ");
                    }
                } catch (Exception e) {
                }
            }
            else{
                ;
            }
            appendToString("\n");
        });


        return string;
    }
}
