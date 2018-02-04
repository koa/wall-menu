package ch.bergturbenthal.home.service.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tinkerforge.AlreadyConnectedException;
import com.tinkerforge.Device;
import com.tinkerforge.DeviceFactory;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NetworkException;
import com.tinkerforge.NotConnectedException;

import ch.bergturbenthal.home.TinkerforgeProperties;
import ch.bergturbenthal.home.service.TinkerforgeDiscovery;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

@Slf4j
@Service
public class DefaultTinkerforgeDiscovery implements TinkerforgeDiscovery {

    @Value
    private static class ListenerRegistration<D extends Device> {
        private Class<D>             registrationType;
        private DiscoveryListener<D> listener;
    }

    @Value
    private static class RunningConnection {
        private IPConnection                            connection;
        private AtomicBoolean                           connected = new AtomicBoolean(false);
        private AtomicReference<Map<TIdentity, Device>> devices   = new AtomicReference<>(Collections.emptyMap());
    }

    @Autowired
    private TinkerforgeProperties                                           properties;
    private final Map<InetAddress, RunningConnection>                       kownConnections = new HashMap<>();
    private final AtomicReference<Collection<ListenerRegistration<Device>>> listeners       = new AtomicReference<>(Collections.emptyList());

    @Override
    public <D extends Device> Disposable addDiscoveryListener(final Class<D> type, final DiscoveryListener<D> listener) {
        @SuppressWarnings("unchecked")
        final ListenerRegistration<Device> listenerRegistration = new ListenerRegistration<>((Class<Device>) type,
                (DiscoveryListener<Device>) listener);
        listeners.updateAndGet(oldList -> {
            final ArrayList<ListenerRegistration<Device>> newList = new ArrayList<>(oldList);
            newList.add(listenerRegistration);
            return Collections.unmodifiableList(newList);
        });
        kownConnections.values().stream().filter(e -> e.getConnected().get()).flatMap(e -> e.getDevices().get().entrySet().stream())
                .forEach(deviceEntry -> publishMutationToListener(listenerRegistration,
                        new DeviceMutation<>(deviceEntry.getKey(), deviceEntry.getValue(), DeviceMutationType.ADDED)));
        return () -> {
            listeners.updateAndGet(oldList -> {
                final ArrayList<ListenerRegistration<Device>> newList = new ArrayList<>(oldList);
                final boolean removed = newList.remove(listenerRegistration);
                if (removed) {
                    return Collections.unmodifiableList(newList);
                } else {
                    return oldList;
                }
            });
        };
    }

    private synchronized void checkAddress(final InetAddress hostAddress) throws IOException {
        if (hostAddress.isReachable(5)) {
            final RunningConnection existingConnection = kownConnections.get(hostAddress);
            if (existingConnection == null) {
                try {
                    final IPConnection ipConnection = new IPConnection();
                    final RunningConnection connection = new RunningConnection(ipConnection);
                    ipConnection.addEnumerateListener(
                            (uid, connectedUid, position, hardwareVersion, firmwareVersion, deviceIdentifier, enumerationType) -> {

                                if (enumerationType == IPConnection.ENUMERATION_TYPE_DISCONNECTED) {
                                    final TIdentity identity = new TIdentity(hostAddress, uid, uid, position, hardwareVersion[0], hardwareVersion[1],
                                            hardwareVersion[2], firmwareVersion[0], firmwareVersion[1], firmwareVersion[2], deviceIdentifier);
                                    final Map<TIdentity, Device> dataBefore = connection.getDevices().getAndUpdate(m -> {
                                        final Map<TIdentity, Device> newMap = new HashMap<>(m);
                                        if (newMap.remove(identity) == null) {
                                            return m;
                                        }
                                        return Collections.unmodifiableMap(newMap);
                                    });
                                    final Device deviceData = dataBefore.get(identity);
                                    if (deviceData != null) {
                                        deviceRemoved(identity, deviceData);
                                    }
                                } else {
                                    try {
                                        final Device device = DeviceFactory.createDevice(deviceIdentifier, uid, ipConnection);
                                        final TIdentity identity = TIdentity.fromIdentity(device.getIdentity(), hostAddress);
                                        final Map<TIdentity, Device> devicesBefore = connection.getDevices().getAndUpdate(m -> {
                                            final Map<TIdentity, Device> newMap = new HashMap<>(m);
                                            newMap.put(identity, device);
                                            return Collections.unmodifiableMap(newMap);
                                        });
                                        if (!devicesBefore.containsKey(uid)) {
                                            deviceAdded(identity, device);
                                        }
                                    } catch (final Exception e) {
                                        log.error("Cannot create device of " + uid + " at " + hostAddress);
                                    }
                                }

                            });
                    ipConnection.addDisconnectedListener(disconnectReason -> {
                        log.info("Disconnected to " + hostAddress);
                        connection.getConnected().set(false);
                        final Map<TIdentity, Device> devicesBefore = connection.getDevices().getAndSet(Collections.emptyMap());
                        for (final Entry<TIdentity, Device> entry : devicesBefore.entrySet()) {
                            deviceRemoved(entry.getKey(), entry.getValue());
                        }
                    });
                    ipConnection.addConnectedListener(connectReason -> {
                        log.info("Connected to " + hostAddress);
                        connection.getConnected().set(true);
                        try {
                            ipConnection.enumerate();
                        } catch (final NotConnectedException e) {
                            log.error("Cannot enumerate after connect on " + hostAddress, e);
                        }
                    });
                    ipConnection.connect(hostAddress.getHostAddress(), 4223);
                    kownConnections.put(hostAddress, connection);
                } catch (NetworkException | AlreadyConnectedException e) {
                    // throw new RuntimeException("Cannot connect " + hostAddress, e);
                } catch (final Exception ex) {
                    log.warn("Exception", ex);
                }

            }
        }
    }

    private void deviceAdded(final TIdentity identity, final Device device) {
        log.info("Found: " + device);
        publishMutation(new DeviceMutation<>(identity, device, DeviceMutationType.ADDED));

    }

    private void deviceRemoved(final TIdentity identity, final Device device) {
        log.info("Removed: " + device);
        publishMutation(new DeviceMutation<>(identity, device, DeviceMutationType.REMOVED));
    }

    private void publishMutation(final DeviceMutation<Device> deviceMutation) {
        for (final ListenerRegistration<Device> listener : listeners.get()) {
            publishMutationToListener(listener, deviceMutation);
        }
    }

    private void publishMutationToListener(final ListenerRegistration<Device> listener, final DeviceMutation<Device> deviceMutation) {
        final Class<Device> registrationType = listener.getRegistrationType();
        if (registrationType.isInstance(deviceMutation.getDevice())) {
            try {
                listener.getListener().deviceMutated(deviceMutation);
            } catch (final Exception ex) {
                log.error("Cannot send mutation to listener", ex);
            }
        }
    }

    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 1000)
    public void scanNewEndpoints() throws IOException {
        log.info("Start probe");
        for (final String discoveryAddress : properties.getDiscovery()) {
            final String[] addressParts = discoveryAddress.split("/");
            if (addressParts.length == 1) {
                final InetAddress hostAddress = InetAddress.getByName(addressParts[0]);
                checkAddress(hostAddress);
            } else {
                final InetAddress netAddress = InetAddress.getByName(addressParts[0]);
                final int netmask = Integer.parseInt(addressParts[1]);
                final BigInteger baseAddr = new BigInteger(netAddress.getAddress());
                final int length = netAddress.getAddress().length * 8;
                final BigInteger mask = BigInteger.ONE.shiftLeft(netmask).subtract(BigInteger.ONE).shiftLeft(length - netmask);
                final BigInteger firstAddress = baseAddr.and(mask);
                final BigInteger lastAddress = baseAddr.or(BigInteger.ONE.shiftLeft(length - netmask).subtract(BigInteger.ONE));
                // log.info("Last Address: " + InetAddress.getByAddress(lastAddress.toByteArray()));
                for (BigInteger address = firstAddress; address.compareTo(lastAddress) <= 0; address = address.add(BigInteger.ONE)) {
                    final InetAddress hostAddress = InetAddress.getByAddress(address.toByteArray());
                    checkAddress(hostAddress);
                }
            }
        }
        log.info("Finished probe");
    }
}
