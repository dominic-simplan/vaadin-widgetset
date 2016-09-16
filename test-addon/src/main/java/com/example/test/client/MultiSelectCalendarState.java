package com.example.test.client;

import java.util.Date;
import java.util.List;

import com.vaadin.shared.AbstractFieldState;
import com.vaadin.shared.annotations.DelegateToWidget;

public class MultiSelectCalendarState extends AbstractFieldState {

	@DelegateToWidget
	public Date month;

	@DelegateToWidget
	public List<Date> monthDays;

}