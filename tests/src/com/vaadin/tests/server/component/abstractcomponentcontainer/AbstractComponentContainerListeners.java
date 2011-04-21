package com.vaadin.tests.server.component.abstractcomponentcontainer;

import com.vaadin.tests.server.component.ListenerMethods;
import com.vaadin.ui.ComponentContainer.ComponentAttachEvent;
import com.vaadin.ui.ComponentContainer.ComponentAttachListener;
import com.vaadin.ui.ComponentContainer.ComponentDetachEvent;
import com.vaadin.ui.ComponentContainer.ComponentDetachListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

public class AbstractComponentContainerListeners extends ListenerMethods {
    public void testComponentDetachListenerAddGetRemove() throws Exception {
        testListenerAddGetRemove(HorizontalLayout.class,
                ComponentDetachEvent.class, ComponentDetachListener.class);
    }

    public void testComponentAttachListenerAddGetRemove() throws Exception {
        testListenerAddGetRemove(VerticalLayout.class,
                ComponentAttachEvent.class, ComponentAttachListener.class);
    }
}