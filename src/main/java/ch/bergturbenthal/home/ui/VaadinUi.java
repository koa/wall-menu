package ch.bergturbenthal.home.ui;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringUI
@Theme("valo")
@Push
@Widgetset("ch.bergturbenthal.home.ui.WidgetSet")
public class VaadinUi extends UI {
    @Autowired
    private SpringNavigator    springNavigator;
    @Autowired
    private SpringViewProvider viewProvider;
    // @Autowired
    // private AuthenticatedUser user;

    @Override
    protected void init(final VaadinRequest vaadinRequest) {

        final HorizontalLayout leftHeader = new HorizontalLayout();
        final HorizontalLayout rightHeader = new HorizontalLayout();

        final HorizontalLayout header = new HorizontalLayout(leftHeader, rightHeader);
        header.setWidth(100, Unit.PERCENTAGE);
        header.setComponentAlignment(rightHeader, Alignment.TOP_RIGHT);
        header.setMargin(new MarginInfo(false, true));
        header.setExpandRatio(leftHeader, 1);
        // leftHeader.setSizeFull();
        header.setExpandRatio(rightHeader, 1);
        // rightHeader.setSizeFull();
        final Panel main = new Panel();
        final HorizontalLayout footer = new HorizontalLayout();
        footer.setWidth(100, Unit.PERCENTAGE);
        // leftHeader.addComponent(new Label(user.getName()));

        final VerticalLayout authorityList = new VerticalLayout();
        authorityList.setMargin(false);
        // for (final GrantedAuthority authority : user.getAuthorities()) {
        // authorityList.addComponent(new Label(authority.getAuthority()));
        // }
        rightHeader.addComponent(authorityList);

        final Optional<URL> pictureUrl = Optional.empty();
        // final Optional<URL> pictureUrl = user.getPicture().flatMap(u -> {
        // try {
        // return Optional.of(u.toURL());
        // } catch (final MalformedURLException e) {
        // log.error("Error decoding picture url", e);
        // return Optional.empty();
        // }
        // });

        final MenuBar leftMenu = new MenuBar();
        for (final String viewName : viewProvider.getViewNamesForCurrentUI()) {
            String label;
            if (viewName.isEmpty()) {
                label = "Home";
            } else {
                label = viewName;
            }
            leftMenu.addItem(label, (menuEvent) -> {
                getNavigator().navigateTo(viewName);
            });
        }
        leftHeader.addComponent(leftMenu);

        final MenuBar rightMenu = new MenuBar();
        final MenuItem settingsMenu = rightMenu.addItem("Settings", null);
        settingsMenu.addItem("logout", menuEvent -> {
            final Page page = getUI().getPage();
            final URI location = page.getLocation();
            page.setLocation(location.resolve("logout"));

        });
        rightHeader.addComponent(rightMenu);
        // menuBar.addi

        final Optional<ExternalResource> pictureResource = pictureUrl.map(url -> new ExternalResource(url));
        pictureResource.ifPresent(resource -> {
            final Image userPicture = new Image();
            userPicture.setSource(resource);
            userPicture.setHeight(8, Unit.EX);
            rightHeader.addComponent(userPicture);
        });
        // footer.addComponent(new Label("Footer"));
        final VerticalLayout mainLayout = new VerticalLayout(header, main, footer);
        mainLayout.setSizeFull();
        mainLayout.setExpandRatio(main, 1);
        main.setSizeFull();
        mainLayout.setMargin(false);
        springNavigator.init(this, view -> main.setContent(view.getViewComponent()));
        setNavigator(springNavigator);
        setContent(mainLayout);
    }
}
