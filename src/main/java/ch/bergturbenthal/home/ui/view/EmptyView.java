package ch.bergturbenthal.home.ui.view;

import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SpringView(name = "")
public class EmptyView extends CustomComponent implements View {
    public EmptyView() {
        final VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.addComponent(new Label("Empty"));
        setCompositionRoot(rootLayout);

    }
}
