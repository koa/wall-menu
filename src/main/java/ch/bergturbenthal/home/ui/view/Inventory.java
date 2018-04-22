package ch.bergturbenthal.home.ui.view;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.tinkerforge.BrickletOLED128x64;
import com.tinkerforge.Device;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.contextmenu.ContextMenu.ContextMenuOpenListener;
import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.contextmenu.MenuItem;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ContextClickEvent;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.GridContextClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import ch.bergturbenthal.home.display.DisplayContent;
import ch.bergturbenthal.home.service.TinkerforgeDiscovery;
import ch.bergturbenthal.home.service.TinkerforgeDiscovery.TIdentity;
import ch.bergturbenthal.home.service.impl.Oled128x64Display;
import ch.bergturbenthal.home.service.impl.TableTextDisplayContent;
import ch.bergturbenthal.home.service.impl.TableTextDisplayContent.DisplayText;
import ch.bergturbenthal.home.service.impl.TableTextDisplayContent.LeftRightTextRow;
import ch.bergturbenthal.home.service.impl.TableTextDisplayContent.TextDisplayContent;
import ch.bergturbenthal.home.service.impl.TableTextDisplayContent.TextRow;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Slf4j
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
        foundComponentsGrid.addColumn(i -> i.getConnectionAddress().getHostAddress()).setCaption("Connection");

        final GridContextMenu<TIdentity> contextMenu = new GridContextMenu<>(foundComponentsGrid);
        final Map<TIdentity, TableTextDisplayContent> renderers = new HashMap<>();
        final Function<TIdentity, TableTextDisplayContent> rendererOfItem = item -> {
            return renderers.computeIfAbsent(item, k -> new TableTextDisplayContent(new TextDisplayContent() {

                @Override
                public List<TextRow> renderText(final int maxRowCount) {
                    return Arrays.<TextRow> asList(
                            LeftRightTextRow.builder().leftAfterAlign(DisplayText.plainText("UID")).rightAfterAlign(DisplayText.boldText(k.getUid()))
                                    .build(),
                            LeftRightTextRow.builder().leftAfterAlign(DisplayText.plainText("HW"))
                                    .rightAfterAlign(DisplayText.boldText(k.getHwVersion())).inverted(true).build(),
                            LeftRightTextRow.builder().leftAfterAlign(DisplayText.plainText("FW"))
                                    .rightAfterAlign(DisplayText.boldText(k.getFwVersion())).build(),
                            LeftRightTextRow.builder().leftAfterAlign(DisplayText.plainText("IP"))
                                    .rightAfterAlign(DisplayText.boldText(k.getConnectionAddress().getHostAddress())).build(),
                            LeftRightTextRow.builder().leftAfterAlign(DisplayText.plainText("Hello world")).build());
                }
            }));
        };

        contextMenu.addContextMenuOpenListener(new ContextMenuOpenListener() {
            @Override
            public void onContextMenuOpen(final ContextMenuOpenEvent openEvent) {
                final ContextMenu menu = openEvent.getContextMenu();
                menu.removeItems();
                final ContextClickEvent contextClickEvent = openEvent.getContextClickEvent();
                final TIdentity item = ((GridContextClickEvent<TIdentity>) contextClickEvent).getItem();
                final Device device = knownDevices.get(item);
                if (device == null) {
                    return;
                }
                if (device instanceof BrickletOLED128x64) {
                    final BrickletOLED128x64 oledDevice = (BrickletOLED128x64) device;
                    final Oled128x64Display display = new Oled128x64Display(oledDevice);
                    menu.addItem("Identify", selectedItem -> {
                        final TableTextDisplayContent renderer = rendererOfItem.apply(item);
                        display.setDisplayContent(Mono.<DisplayContent> just(renderer).flux());
                    });
                    final MenuItem fontMenu = menu.addItem("Font", null);
                    for (final String fontName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                        fontMenu.addItem(fontName, selectedItem -> {
                            final TableTextDisplayContent renderer = rendererOfItem.apply(item);
                            renderer.setFontName(fontName);
                            display.setDisplayContent(Mono.<DisplayContent> just(renderer).flux());
                        });
                    }

                    final MenuItem fontSizeMenu = menu.addItem("Font-Size", null);
                    for (int i = 8; i < 20; i++) {
                        final int fontSize = i;
                        fontSizeMenu.addItem("" + i, selectedItem -> {
                            final TableTextDisplayContent renderer = rendererOfItem.apply(item);
                            renderer.setFontSize(fontSize);
                            display.setDisplayContent(Mono.<DisplayContent> just(renderer).flux());
                        });
                    }
                    final MenuItem brightnessMenu = menu.addItem("Brightness", null);
                    for (int i = 0; i < 10; i++) {
                        final short br = (short) (i * 256 / 10);
                        brightnessMenu.addItem("" + i, selectedItem -> {
                            try {
                                oledDevice.setDisplayConfiguration(br, false);
                            } catch (TimeoutException | NotConnectedException e) {
                                log.warn("Cannot update", e);
                            }
                        });
                    }
                }
                // menu.markAsDirty();
                // log.info("Source: " + item);
            }
        });
        contextMenu.addItem("Show dummy Text", event -> {
            // final ContextClickEvent contextClickEvent = event. .getContextClickEvent();
            // final TIdentity item = ((GridContextClickEvent<TIdentity>) contextClickEvent).getItem();
            // final Device device = knownDevices.get(item);
            // if (device == null) {
            // return;
            // }
            // if (device instanceof BrickletOLED128x64) {
            // final Oled128x64Display display = new Oled128x64Display((BrickletOLED128x64) device);
            // final TextRenderer textRenderer = new TextRenderer() {
            //
            // @Override
            // public List<TextRow> renderText(final int maxRowCount) {
            // return Arrays.<TextRow> asList(LeftRightTextRow.builder().leftAfterAlign("Hello world").build());
            // }
            // };
            // final DisplayRenderer r = new TextDisplayRenderer(textRenderer);
            // display.draw(r);
            // }
        });

        foundComponentsGrid.setSizeFull();
        rootLayout.setExpandRatio(foundComponentsGrid, 1f);
        rootLayout.setSizeFull();
        setSizeFull();
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
                    foundComponentsGrid.recalculateColumnWidths();
                }))));
        addDetachListener(event -> disposableConsumer.accept(null));
    }
}
