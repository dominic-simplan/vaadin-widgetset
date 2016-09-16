package com.example.test.client;

import com.example.test.MultiSelectCalendar;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

@Connect(MultiSelectCalendar.class)
public class MultiSelectCalendarConnector extends AbstractComponentConnector {

	public MultiSelectCalendarConnector() {
	}

	@Override
	public void onStateChanged(StateChangeEvent stateChangeEvent) {
		super.onStateChanged(stateChangeEvent);
	}

	@Override
	public VMultiSelectCalendarWidget getWidget() {
		return (VMultiSelectCalendarWidget) super.getWidget();
	}

	@Override
	public MultiSelectCalendarState getState() {
		return (MultiSelectCalendarState) super.getState();
	}

}
