package com.example.test;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.example.test.client.MultiSelectCalendarState;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.AbstractField;

public class MultiSelectCalendar extends AbstractField<List> {

	public MultiSelectCalendar() {
		super();
		//		registerRpc(new MultiSelectCalendarServerRpc() {
		//
		//			@Override
		//			public void setMonthDays(List<Date> monthDays) {
		//				setValue(monthDays);
		//			}
		//		});
	}

	public MultiSelectCalendar(Month month, int year) {
		this();
		getState().month = Date.from(LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public void setValue(List monthDays) throws ReadOnlyException, ConversionException {
		super.setValue(monthDays);
		//getState().monthDays = monthDays;
	}

	@Override
	public MultiSelectCalendarState getState() {
		return (MultiSelectCalendarState) super.getState();
	}

	@Override
	public Class<List> getType() {
		return List.class;
	}

}
