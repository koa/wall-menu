package ch.bergturbenthal.home.service;

import com.tinkerforge.Device;
import com.tinkerforge.Device.Identity;

import lombok.Value;
import reactor.core.Disposable;

public interface TinkerforgeDiscovery {
    @Value
    public static class DeviceMutation<D extends Device> {
        private TIdentity          identity;
        private D                  device;
        private DeviceMutationType mutation;
    }

    public enum DeviceMutationType {
        ADDED, REMOVED;
    }

    @FunctionalInterface
    public interface DiscoveryListener<D extends Device> {
        void deviceMutated(DeviceMutation<D> mutation);
    }

    @Value
    public static class TIdentity {
        public static TIdentity fromIdentity(final Identity id) {
            return new TIdentity(id.uid, id.connectedUid, id.position, id.hardwareVersion[0], id.hardwareVersion[1], id.hardwareVersion[2],
                    id.firmwareVersion[0], id.firmwareVersion[1], id.firmwareVersion[2], id.deviceIdentifier);
        }

        private String uid;
        private String connectedUid;
        private char   position;
        private short  hardwareVersion0;
        private short  hardwareVersion1;
        private short  hardwareVersion2;
        private short  firmwareVersion0;
        private short  firmwareVersion1;
        private short  firmwareVersion2;
        private int    deviceIdentifier;

        public String getFwVersion() {
            return firmwareVersion0 + "." + firmwareVersion1 + "." + firmwareVersion2;
        }

        public String getHwVersion() {
            return hardwareVersion0 + "." + hardwareVersion1 + "." + hardwareVersion2;
        }

        @Override
        public String toString() {
            return "[" + "uid = " + uid + ", " + "connectedUid = " + connectedUid + ", " + "position = " + position + ", " + "hardwareVersion = "
                    + getHwVersion() + ", " + "firmwareVersion = " + getFwVersion() + ", " + "deviceIdentifier = " + deviceIdentifier + "]";
        }
    }

    <D extends Device> Disposable addDiscoveryListener(Class<D> type, DiscoveryListener<D> listener);
}
