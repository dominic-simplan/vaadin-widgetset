package com.example.test;

import java.time.Month;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@SpringUI
@Theme("mytheme")
public class MyUI extends UI {

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();
		final TextField name = new TextField();
		name.setCaption("Type your name here:");

		// This is a component from the test-addon module
		layout.addComponent(new MultiSelectCalendar(Month.APRIL, 2016));
		layout.addComponents(name/* , button, grid */);
		layout.setSizeFull();
		layout.setMargin(true);
		layout.setSpacing(true);

		setContent(layout);
	}
}
