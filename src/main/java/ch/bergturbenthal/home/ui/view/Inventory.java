package ch.bergturbenthal.home.ui.view;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.tinkerforge.Device;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import ch.bergturbenthal.home.service.TinkerforgeDiscovery;
import ch.bergturbenthal.home.service.TinkerforgeDiscovery.TIdentity;
import reactor.core.Disposable;

@SpringView(name = "Inventory")
public class Inventory extends CustomComponent implements View {
    public Inventory(final TinkerforgeDiscovery tinkerforgeDiscovery) {
        final VerticalLayout rootLayout = new VerticalLayout();
        final Map<TIdentity, Device> knownDevices = new HashMap<>();
        final ListDataProvider<TIdentity> dataProvider = DataProvider.ofCollection(knownDevices.keySet());
        final Grid<TIdentity> foundComponentsGrid = new Grid<TIdentity>(dataProvider);
        rootLayout.addComponent(new Label("Inventory"));
        rootLayout.addComponent(foundComponentsGrid);
        final AtomicReference<Disposable> currentOpenDisposable = new AtomicReference<Disposable>(null);
        final Consumer<Disposable> disposableConsumer = disposable -> {
            final Disposable oldDisposable = currentOpenDisposable.getAndSet(disposable);
            if (oldDisposable != null) {
                oldDisposable.dispose();
            }
        };

        foundComponentsGrid.addColumn(i -> knownDevices.get(i).getClass().getSimpleName()).setCaption("Type");
        foundComponentsGrid.addColumn(i -> i.getUid()).setCaption("UID");
        foundComponentsGrid.addColumn(i -> i.getHwVersion()).setCaption("Hardware");
        foundComponentsGrid.addColumn(i -> i.getFwVersion()).setCaption("Firmware");

        setCompositionRoot(rootLayout);
        addAttachListener(
                event -> disposableConsumer.accept(tinkerforgeDiscovery.addDiscoveryListener(Device.class, deviceEvent -> getUI().access(() -> {
                    switch (deviceEvent.getMutation()) {
                        case ADDED:
                            knownDevices.put(deviceEvent.getIdentity(), deviceEvent.getDevice());
                            break;
                        case REMOVED:
                            knownDevices.remove(deviceEvent.getIdentity());
                            break;
                    }
                    dataProvider.refreshAll();
                }))));
        addDetachListener(event -> disposableConsumer.accept(null));
    }
}
