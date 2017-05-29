package fom;

import hla.rti.AttributeHandleSet;
import hla.rti.RTIinternalError;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by konrad on 5/29/17.
 */
public abstract class AFomEntity {
    private int classHandle;
    private Map<String, Integer> namesToHandles;
    private Map<Integer, Class<?>> handlesToTypes;
    private Map<Integer, String> handlesToNames;

    public AFomEntity(int classHandle) {
        this.classHandle = classHandle;
        namesToHandles = new HashMap<>();
        handlesToNames = new HashMap<>();
        handlesToTypes = new HashMap<>();
    }

    public int getClassHandle() {
        return classHandle;
    }

    public void addAttributeHandle(String attributeName, Integer attributeHandle, Class<?> attributeType) {
        namesToHandles.put(attributeName, attributeHandle);
        handlesToTypes.put(attributeHandle, attributeType);
        handlesToNames.put(attributeHandle, attributeName);
    }

    public int getHandleFor(String attributeName) {
        return namesToHandles.get(attributeName);
    }

    public String getNameFor(int attributeHandle) {
        return handlesToNames.get(attributeHandle);
    }

    public Class<?> getTypeFor(int attributeHandle) {
        return handlesToTypes.get(attributeHandle);
    }

    public AttributeHandleSet createAttributeHandleSet() throws RTIinternalError {
        AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        namesToHandles.values().forEach(value -> {
            try {
                attributes.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return attributes;
    }
}